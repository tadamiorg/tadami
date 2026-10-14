package com.sf.tadami.extension

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.sf.tadami.R
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.api.ExtensionsApi
import com.sf.tadami.extension.api.RepoExtensions
import com.sf.tadami.extension.model.ExtensionLoadIssue
import com.sf.tadami.extension.model.InstallStep
import com.sf.tadami.extension.model.LoadResult
import com.sf.tadami.extension.util.ExtensionsInstaller
import com.sf.tadami.extension.util.ExtensionsLoader
import com.sf.tadami.extension.util.ExtensionsMigration
import com.sf.tadami.extension.util.ExtensionsStorage
import com.sf.tadami.notifications.extensionsinstaller.ExtensionInstallerNotifier
import com.sf.tadami.preferences.extensions.TrustedExtensionsPreferences
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.source.StubSource
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.utils.editPreference
import com.sf.tadami.utils.getPreferencesGroup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class ExtensionManager(
    private val context: Context,
    private val dataStore: DataStore<Preferences> = Injekt.get()
) {
    var isInitialized = false
        private set

    private val api = ExtensionsApi()

    private val sourcesPreferences
        get() = runBlocking {
            dataStore.getPreferencesGroup(SourcesPreferences)
        }

    /**
     * The installer which installs and updates the extensions.
     */
    private val installer by lazy { ExtensionsInstaller(context) }

    private val storageDir: File = ExtensionsStorage.dir(context)

    /** Runs the installs and uninstalls, which go on without the screen that started them. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** The installs running, by package name. */
    private val installs = ConcurrentHashMap<String, Install>()

    private class Install(val job: Job, val steps: MutableStateFlow<InstallStep>)

    private val _installedExtensionsFlow = MutableStateFlow(emptyList<Extension.Installed>())
    val installedExtensionsFlow = _installedExtensionsFlow.asStateFlow()

    private val _legacyPackagesFlow = MutableStateFlow(emptyList<String>())

    /** The extensions still installed as apps, as they were before, that are copied, so not needed. */
    val legacyPackagesFlow = _legacyPackagesFlow.asStateFlow()

    /** The icon loaded with the extension of the source [sourceId]. */
    fun getAppIconForSource(sourceId: Long): Drawable? {
        return _installedExtensionsFlow.value
            .find { ext -> ext.sources.any { it.id == sourceId } }
            ?.icon
    }

    private val _availableExtensionsFlow = MutableStateFlow(emptyList<Extension.Available>())
    val availableExtensionsFlow = _availableExtensionsFlow.asStateFlow()

    private var availableExtensionsSourcesData: Map<Long, StubSource> = emptyMap()

    private fun setupAvailableExtensionsSourcesDataMap(extensions: List<Extension.Available>) {
        if (extensions.isEmpty()) return
        availableExtensionsSourcesData = extensions
            .flatMap { ext -> ext.sources.map { it.toStubSource() } }
            .associateBy { it.id }
    }

    fun getSourceData(id: Long) = availableExtensionsSourcesData[id]

    init {
        initExtensions()
        instance = this
    }

    companion object {
        /**
         * The manager once built, for the crash report: getting it from Injekt would build it, and
         * load the extensions again, from the uncaught exception handler.
         */
        // Built with the application context, it lives as long as the app anyway
        @SuppressLint("StaticFieldLeak")
        @Volatile
        var instance: ExtensionManager? = null
            private set
    }

    private fun initExtensions() {
        ExtensionsStorage.cleanTmp(storageDir)
        // Before the extensions are loaded, so that the ones still installed as apps are loaded
        // from their copy. A failure must not prevent the others from loading: retried next launch
        _legacyPackagesFlow.value = runCatching { runBlocking { ExtensionsMigration.migrate(context, dataStore) } }
            .onFailure { Log.d("ExtensionsMigration", it.stackTraceToString()) }
            .getOrDefault(emptyList())

        val extensions = ExtensionsLoader.loadExtensions(context, ::onLateExtensionLoaded)

        val loadedExtensions = extensions
            .filterIsInstance<LoadResult.Success>()
            .map { it.extension }
        _installedExtensionsFlow.update { lateExtensions ->
            // One that timed out may have finished loading already
            loadedExtensions.map { extension ->
                lateExtensions.find { it.pkgName == extension.pkgName }
                    ?.takeIf { extension.loadIssue == ExtensionLoadIssue.TIMED_OUT }
                    ?: extension
            }
        }

        isInitialized = true
    }

    /** Registers an extension that finished loading after its time limit. */
    private fun onLateExtensionLoaded(result: LoadResult) {
        val extension = (result as? LoadResult.Success)?.extension ?: return
        _installedExtensionsFlow.update { installedExtensions ->
            val current = installedExtensions.find { it.pkgName == extension.pkgName }
            // Already replaced, by an update for instance, or uninstalled
            if (current != null && current.loadIssue != ExtensionLoadIssue.TIMED_OUT ||
                !ExtensionsStorage.apkFile(storageDir, extension.pkgName).isFile
            ) {
                installedExtensions
            } else {
                installedExtensions.filterNot { it.pkgName == extension.pkgName } + extension.withUpdateCheck()
            }
        }
        updatePendingUpdatesCount()
    }

    suspend fun findAvailableExtensions() {
        val extensions: RepoExtensions = try {
            api.findExtensions()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d("FindAvailableExtensions", e.stackTraceToString())
            withContext(Dispatchers.Main) {
                UiToasts.showToast(R.string.extension_api_error)
            }
            // Keep the previous available extensions, statuses and updates count
            return
        }
        _availableExtensionsFlow.value = extensions.available
        updatedInstalledExtensionsStatuses(extensions)
        setupAvailableExtensionsSourcesDataMap(extensions.available)
    }

    /**
     * Sets the update field of the installed extensions with the given [repoExtensions].
     *
     * @param repoExtensions The extensions given by the [api].
     */
    private fun updatedInstalledExtensionsStatuses(repoExtensions: RepoExtensions) {
        val availableExtensions = repoExtensions.available
        // Nothing to compare against, keep the current statuses and updates count
        if (availableExtensions.isEmpty()) return

        _installedExtensionsFlow.update { installedExtensions ->
            val mutInstalledExtensions = installedExtensions.toMutableList()
            var changed = false

            for ((index, installedExt) in mutInstalledExtensions.withIndex()) {
                val pkgName = installedExt.pkgName
                val availableExt = availableExtensions.find { it.pkgName == pkgName }

                if (availableExt == null) {
                    val isObsolete = pkgName !in repoExtensions.listedPkgNames
                    if (installedExt.isObsolete != isObsolete) {
                        mutInstalledExtensions[index] = installedExt.copy(isObsolete = isObsolete)
                        changed = true
                    }
                } else {
                    val hasUpdate = installedExt.updateExists(availableExt)

                    if (installedExt.hasUpdate != hasUpdate ||
                        installedExt.isObsolete ||
                        installedExt.repoUrl != availableExt.repoUrl
                    ) {
                        mutInstalledExtensions[index] = installedExt.copy(
                            hasUpdate = hasUpdate,
                            isObsolete = false,
                            repoUrl = availableExt.repoUrl,
                        )
                        changed = true
                    }
                }
            }
            if (changed) mutInstalledExtensions else installedExtensions
        }
        updatePendingUpdatesCount()
    }

    /**
     * Returns a flow of the installation process for the given extension. It will complete
     * once the extension is installed or fails, or the install is cancelled. The install goes on
     * if unsubscribed before its completion.
     *
     * @param extension The extension to be installed.
     */
    fun installExtension(extension: Extension.Available): Flow<InstallStep> {
        val listed = listedExtension(extension) ?: return emptyFlow()
        return startInstall(listed).transformWhile {
            emit(it)
            !it.isCompleted()
        }
    }

    /**
     * Returns a flow of the installation process for the given extension. It will complete
     * once the extension is updated or fails, or the update is cancelled. The update goes on
     * if unsubscribed before its completion.
     *
     * @param extension The extension to be updated.
     */
    fun updateExtension(extension: Extension.Installed): Flow<InstallStep> {
        val availableExt = _availableExtensionsFlow.value.find { it.pkgName == extension.pkgName }
            ?: return emptyFlow()
        return installExtension(availableExt)
    }

    fun cancelInstallUpdateExtension(extension: Extension) {
        val pkgName = when (extension) {
            is Extension.Available -> listedExtension(extension)?.pkgName
            is Extension.Installed -> extension.pkgName
        }
        installs[pkgName ?: return]?.job?.cancel()
    }

    /**
     * The repo entry of [extension]: the extensions list has a copy of it per source, with its own
     * package name.
     */
    private fun listedExtension(extension: Extension.Available): Extension.Available? {
        return _availableExtensionsFlow.value.find {
            it.repoUrl == extension.repoUrl && it.apkName == extension.apkName
        }
    }

    /** Starts the install of [listed], or returns the steps of the one running for its package. */
    private fun startInstall(listed: Extension.Available): StateFlow<InstallStep> {
        val pkgName = listed.pkgName
        val steps = MutableStateFlow(InstallStep.Pending)
        val job = scope.launch(start = CoroutineStart.LAZY) { steps.value = install(listed, steps) }
        val install = Install(job, steps)
        installs.putIfAbsent(pkgName, install)?.let { running ->
            job.cancel()
            return running.steps
        }
        job.invokeOnCompletion {
            // Cancelled, it still ends with a step for the screens that wait for it
            if (!steps.value.isCompleted()) steps.value = InstallStep.Idle
            installs.remove(pkgName, install)
        }
        job.start()
        return steps
    }

    /**
     * Downloads [listed] and verifies it, then loads it in place of the installed one.
     *
     * @return The last step of the install.
     */
    private suspend fun install(listed: Extension.Available, steps: MutableStateFlow<InstallStep>): InstallStep {
        val pkgName = listed.pkgName
        var tmp: File? = null
        try {
            val file = installer.newTmpFile(pkgName).also { tmp = it }
            steps.value = InstallStep.Downloading
            installer.download(api.getApkUrl(listed), file)

            steps.value = InstallStep.Installing
            val rejection = ExtensionsStorage.withLock(pkgName) {
                val installed = _installedExtensionsFlow.value.find { it.pkgName == pkgName }
                // The same version can fix one that can't be used
                val allowSameVersion = installed == null || installed.loadIssue != null
                installer.verifyAndCommit(file, pkgName, listed, allowSameVersion).also {
                    if (it == null) reloadExtension(pkgName)
                }
            }
            if (rejection != null) {
                Log.d("InstallExtension", "$pkgName refused: $rejection")
                UiToasts.showToast(rejection.messageRes, Toast.LENGTH_LONG, listed.name)
                return InstallStep.Error
            }
            updatePendingUpdatesCount()
            return InstallStep.Installed
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d("InstallExtension", "Couldn't install $pkgName: ${e.stackTraceToString()}")
            UiToasts.showToast(R.string.ext_install_error_download, Toast.LENGTH_LONG, listed.name)
            return InstallStep.Error
        } finally {
            tmp?.delete()
        }
    }

    /**
     * Uninstalls the given extension.
     *
     * @param extension The extension to uninstall.
     */
    fun uninstallExtension(extension: Extension.Installed) {
        scope.launch {
            ExtensionsStorage.withLock(extension.pkgName) {
                ExtensionsStorage.delete(storageDir, extension.pkgName)
                unregisterExtension(extension.pkgName)
            }
            updatePendingUpdatesCount()
        }
    }

    /** Updates the list of the extension apps not needed anymore, see [legacyPackagesFlow]. */
    suspend fun refreshLegacyPackages(): List<String> {
        return withContext(Dispatchers.IO) { ExtensionsMigration.legacyPackages(context) }
            .also { _legacyPackagesFlow.value = it }
    }

    /** Asks the system to uninstall the extension app [pkgName], see [legacyPackagesFlow]. */
    fun requestUninstallLegacyPackage(pkgName: String) {
        ExtensionsMigration.requestUninstall(context, pkgName)
    }

    /**
     * Trusts the key the given untrusted extension is signed with, then loads the extensions signed
     * with it. They, and their updates signed with it, are loaded from now on.
     *
     * @param extension The extension to trust.
     */
    suspend fun trustExtension(extension: Extension.Installed) {
        val signatureHash = extension.signatureHash ?: return
        dataStore.edit { preferences ->
            preferences[TrustedExtensionsPreferences.TRUSTED_SIGNATURES] =
                preferences[TrustedExtensionsPreferences.TRUSTED_SIGNATURES].orEmpty() + signatureHash
        }
        withContext(Dispatchers.IO) {
            _installedExtensionsFlow.value
                .filter { it.loadIssue == ExtensionLoadIssue.UNTRUSTED && it.signatureHash == signatureHash }
                .forEach { ExtensionsStorage.withLock(it.pkgName) { reloadExtension(it.pkgName) } }
            updatePendingUpdatesCount()
        }
    }

    /**
     * Loads the extension [pkgName] from its file, in place of the registered one. To run under its
     * lock, see [ExtensionsStorage.withLock].
     */
    private fun reloadExtension(pkgName: String) {
        when (val result = ExtensionsLoader.loadExtensionFromPkgName(context, pkgName)) {
            is LoadResult.Success -> registerUpdatedExtension(result.extension.withUpdateCheck())
            LoadResult.Error -> unregisterExtension(pkgName)
        }
    }

    /**
     * Registers the given updated extension in this and the source managers previously removing
     * the outdated ones.
     *
     * @param extension The extension to be registered.
     */
    private fun registerUpdatedExtension(extension: Extension.Installed) {
        _installedExtensionsFlow.update { installedExtensions ->
            val mutInstalledExtensions = installedExtensions.toMutableList()
            val oldExtension = mutInstalledExtensions.find { it.pkgName == extension.pkgName }
            if (oldExtension != null) {
                mutInstalledExtensions -= oldExtension
            }
            mutInstalledExtensions += extension
            mutInstalledExtensions
        }
    }

    /**
     * Unregisters the extension in this and the source managers given its package name.
     *
     * @param pkgName The package name of the uninstalled extension.
     */
    private fun unregisterExtension(pkgName: String) {
        _installedExtensionsFlow.update { installedExtensions ->
            val installedExtension = installedExtensions.find { it.pkgName == pkgName }
            if (installedExtension != null) installedExtensions - installedExtension else installedExtensions
        }
    }

    /**
     * Extension method to set the update field of an installed extension.
     */
    private fun Extension.Installed.withUpdateCheck(): Extension.Installed {
        return if (updateExists()) {
            copy(hasUpdate = true)
        } else {
            this
        }
    }

    private fun Extension.Installed.updateExists(availableExtension: Extension.Available? = null): Boolean {
        val availableExt =
            availableExtension ?: _availableExtensionsFlow.value.find { it.pkgName == pkgName }
            ?: return false

        return (availableExt.versionCode > versionCode || availableExt.apiVersion > apiVersion)
    }

    private fun updatePendingUpdatesCount() {
        // The update statuses aren't known before the available extensions are, keep the count
        // the startup check stored
        if (_availableExtensionsFlow.value.isEmpty()) return
        // An untrusted one is trusted first, then updated
        val pendingUpdateCount = _installedExtensionsFlow.value
            .count { it.hasUpdate && it.loadIssue != ExtensionLoadIssue.UNTRUSTED }
        runBlocking {
            dataStore.editPreference(pendingUpdateCount,SourcesPreferences.EXT_UPDATES_COUNT)
        }
        if (pendingUpdateCount == 0) {
            ExtensionInstallerNotifier(context).dismiss()
        }
    }
}