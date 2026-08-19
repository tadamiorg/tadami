package com.sf.tadami.extension

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.R
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.api.ExtensionsApi
import com.sf.tadami.extension.model.ExtensionsLoadState
import com.sf.tadami.extension.model.InstallStep
import com.sf.tadami.extension.model.LoadResult
import com.sf.tadami.extension.util.ExtensionLoadGuard
import com.sf.tadami.extension.util.ExtensionsInstaller
import com.sf.tadami.extension.util.ExtensionsLoader
import com.sf.tadami.extension.util.ExtensionsLoader.API_VERSION_MIN_OBSOLETE
import com.sf.tadami.notifications.extensionsinstaller.ExtensionInstallerNotifier
import com.sf.tadami.notifications.extensionsinstaller.ExtensionInstallerReceiver
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.source.StubSource
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.utils.editPreference
import com.sf.tadami.utils.logcat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ExtensionManager(
    private val context: Context,
    private val dataStore: DataStore<Preferences> = Injekt.get()
) {
    /**
     * Extensions are third-party code loaded by reflection, so the whole load pass runs here
     * instead of on whatever thread happens to construct this manager (which used to be the main
     * thread, during `Application.onCreate`).
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _loadState = MutableStateFlow<ExtensionsLoadState>(ExtensionsLoadState.Loading)

    /** `Loading` until the initial pass finishes; consumers should not treat an empty list as final. */
    val loadState = _loadState.asStateFlow()

    private val _failedExtensionsFlow = MutableStateFlow(emptyList<LoadResult.Error>())

    /** Packages that looked like extensions but couldn't be loaded, with the reason why. */
    val failedExtensionsFlow = _failedExtensionsFlow.asStateFlow()

    private val api = ExtensionsApi()

    /**
     * The installer which installs, updates and uninstalls the extensions.
     */
    private val installer by lazy { ExtensionsInstaller(context) }
    private val iconMap = mutableMapOf<String, Drawable>()

    private val _installedExtensionsFlow = MutableStateFlow(emptyList<Extension.Installed>())
    val installedExtensionsFlow = _installedExtensionsFlow.asStateFlow()

    fun getAppIconForSource(sourceId: Long): Drawable? {
        val pkgName = _installedExtensionsFlow.value
            .find { ext ->
                ext.sources.any { it.id == sourceId }
            }
            ?.pkgName
            ?: return null

        iconMap[pkgName]?.let { return it }

        // Called from a Compose property getter (domain/source/Source.kt), so a throw here would
        // take down the composition for a missing or corrupt package.
        val icon = runCatching {
            ExtensionsLoader.getExtensionPackageInfoFromPkgName(context, pkgName)
                ?.applicationInfo
                ?.loadIcon(context.packageManager)
        }.getOrNull() ?: return null

        return icon.also { iconMap[pkgName] = it }
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
        instance = this
        scope.launch { initExtensions() }
        ExtensionInstallerReceiver(InstallationListener()).register(context)
    }

    companion object {
        /**
         * The manager, but only if something already built it.
         *
         * The crash reporter wants extension state, and going through Injekt would *create* the
         * manager — kicking off a full extension load from inside the uncaught-exception handler,
         * possibly in the crash-handler process. A plain reference means "report what we know, or
         * report nothing".
         */
        // Not a leak: this is an Injekt application-scoped singleton constructed with the
        // Application itself (AppModule), so it already outlives every Activity.
        @SuppressLint("StaticFieldLeak")
        @Volatile
        var instance: ExtensionManager? = null
            private set
    }

    private suspend fun initExtensions() {
        val extensions = try {
            ExtensionsLoader.loadExtensions(context)
        } catch (e: Throwable) {
            // Never let a loader failure escape: this used to run inside Application.onCreate and
            // took the whole app down with it.
            logcat(LogPriority.ERROR, e) { "Extension loading failed" }
            emptyList()
        }

        _failedExtensionsFlow.value = extensions.filterIsInstance<LoadResult.Error>()

        // Flip to Ready *before* publishing the sources: SourceManager only prunes stale per-source
        // preferences once loading is done, and it does that while collecting this emission.
        _loadState.value = ExtensionsLoadState.Ready

        _installedExtensionsFlow.value = extensions
            .filterIsInstance<LoadResult.Success>()
            .map { it.extension }
    }

    /**
     * Clears the quarantine flag for a previously fatal extension and tries it again.
     * Loading it in isolation means a second hang can't take the other extensions with it.
     */
    fun retryFailedExtension(pkgName: String) {
        scope.launch {
            ExtensionLoadGuard.clearQuarantine(context, pkgName)
            when (val result = ExtensionsLoader.loadExtensionFromPkgName(context, pkgName)) {
                is LoadResult.Success -> {
                    registerUpdatedExtension(result.extension.withUpdateCheck())
                    _failedExtensionsFlow.value =
                        _failedExtensionsFlow.value.filterNot { it.pkgName == pkgName }
                }

                is LoadResult.Error -> {
                    _failedExtensionsFlow.value =
                        _failedExtensionsFlow.value.filterNot { it.pkgName == pkgName } + result
                }
            }
        }
    }

    suspend fun findAvailableExtensions() {
        val extensions: List<Extension.Available> = try {
            api.findExtensions()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to fetch available extensions" }
            withContext(Dispatchers.Main) {
                UiToasts.showToast(R.string.extension_api_error)
            }
            emptyList()
        }
        _availableExtensionsFlow.value = extensions
        updatedInstalledExtensionsStatuses(extensions)
        setupAvailableExtensionsSourcesDataMap(extensions)
    }

    /**
     * Sets the update field of the installed extensions with the given [availableExtensions].
     *
     * @param availableExtensions The list of extensions given by the [api].
     */
    private fun updatedInstalledExtensionsStatuses(availableExtensions: List<Extension.Available>) {
        if (availableExtensions.isEmpty()) {
            scope.launch {
                dataStore.editPreference(0, SourcesPreferences.EXT_UPDATES_COUNT)
            }
            return
        }

        val mutInstalledExtensions = _installedExtensionsFlow.value.toMutableList()
        var changed = false

        for ((index, installedExt) in mutInstalledExtensions.withIndex()) {
            val pkgName = installedExt.pkgName
            val availableExt = availableExtensions.find { it.pkgName == pkgName }

            val apiVersion = installedExt.apiVersion

            if ((availableExt == null || apiVersion < API_VERSION_MIN_OBSOLETE) && !installedExt.isObsolete) {
                mutInstalledExtensions[index] = installedExt.copy(isObsolete = true)
                changed = true
            } else if (availableExt != null) {
                val hasUpdate = installedExt.updateExists(availableExt)

                if (installedExt.hasUpdate != hasUpdate) {
                    mutInstalledExtensions[index] = installedExt.copy(
                        hasUpdate = hasUpdate,
                        repoUrl = availableExt.repoUrl,
                    )
                    changed = true
                } else {
                    mutInstalledExtensions[index] = installedExt.copy(
                        repoUrl = availableExt.repoUrl,
                    )
                    changed = true
                }
            }
        }
        if (changed) {
            _installedExtensionsFlow.value = mutInstalledExtensions
        }
        updatePendingUpdatesCount()
    }

    /**
     * Returns a flow of the installation process for the given extension. It will complete
     * once the extension is installed or throws an error. The process will be canceled if
     * unsubscribed before its completion.
     *
     * @param extension The extension to be installed.
     */
    fun installExtension(extension: Extension.Available): Flow<InstallStep> {
        return installer.downloadAndInstall(api.getApkUrl(extension), extension)
    }

    /**
     * Returns a flow of the installation process for the given extension. It will complete
     * once the extension is updated or throws an error. The process will be canceled if
     * unsubscribed before its completion.
     *
     * @param extension The extension to be updated.
     */
    fun updateExtension(extension: Extension.Installed): Flow<InstallStep> {
        val availableExt = _availableExtensionsFlow.value.find { it.pkgName == extension.pkgName }
            ?: return emptyFlow()
        return installExtension(availableExt)
    }

    fun cancelInstallUpdateExtension(extension: Extension) {
        installer.cancelInstall(extension.pkgName)
    }

    /**
     * Sets to "installing" status of an extension installation.
     *
     * @param downloadId The id of the download.
     */
    fun setInstalling(downloadId: Long) {
        installer.updateInstallStep(downloadId, InstallStep.Installing)
    }

    fun updateInstallStep(downloadId: Long, step: InstallStep) {
        installer.updateInstallStep(downloadId, step)
    }

    /**
     * Uninstalls the extension that matches the given package name.
     *
     * @param extension The extension to uninstall.
     */
    fun uninstallExtension(extension: Extension) {
        uninstallExtension(extension.pkgName)
    }

    /** Uninstall by package, for extensions that failed to load and so have no [Extension]. */
    fun uninstallExtension(pkgName: String) {
        installer.uninstallApk(pkgName)
    }

    /**
     * Registers the given extension in this and the source managers.
     *
     * @param extension The extension to be registered.
     */
    private fun registerNewExtension(extension: Extension.Installed) {
        _installedExtensionsFlow.value += extension
    }

    /**
     * Registers the given updated extension in this and the source managers previously removing
     * the outdated ones.
     *
     * @param extension The extension to be registered.
     */
    private fun registerUpdatedExtension(extension: Extension.Installed) {
        val mutInstalledExtensions = _installedExtensionsFlow.value.toMutableList()
        val oldExtension = mutInstalledExtensions.find { it.pkgName == extension.pkgName }
        if (oldExtension != null) {
            mutInstalledExtensions -= oldExtension
        }
        mutInstalledExtensions += extension
        _installedExtensionsFlow.value = mutInstalledExtensions
    }

    /**
     * Unregisters the extension in this and the source managers given its package name. Note this
     * method is called for every uninstalled application in the system.
     *
     * @param pkgName The package name of the uninstalled application.
     */
    private fun unregisterExtension(pkgName: String) {
        val installedExtension = _installedExtensionsFlow.value.find { it.pkgName == pkgName }

        if (installedExtension != null) {
            _installedExtensionsFlow.value -= installedExtension
        }
    }

    /**
     * Checks if an extension with the given package name and version is already loaded.
     * Returns true if the extension exists and has the same or newer version.
     */
    private fun isExtensionAlreadyLoaded(pkgName: String, versionCode: Long): Boolean {
        val existing = _installedExtensionsFlow.value.find { it.pkgName == pkgName }
        return existing != null && existing.versionCode >= versionCode
    }

    /**
     * Listener which receives events of the extensions being installed, updated or removed.
     */
    private inner class InstallationListener : ExtensionInstallerReceiver.Listener {

        override fun onExtensionInstalled(extension: Extension.Installed) {
            // Check if already loaded to prevent duplicate instantiation
            if (isExtensionAlreadyLoaded(extension.pkgName, extension.versionCode)) {
                logcat { "Extension ${extension.pkgName} already loaded, skipping" }
                return
            }
            logcat { "Registering new extension ${extension.pkgName}" }
            registerNewExtension(extension.withUpdateCheck())
            _failedExtensionsFlow.value =
                _failedExtensionsFlow.value.filterNot { it.pkgName == extension.pkgName }
            updatePendingUpdatesCount()
        }

        override fun onExtensionUpdated(extension: Extension.Installed) {
            val existing = _installedExtensionsFlow.value.find { it.pkgName == extension.pkgName }

            // Only update if version is actually newer
            if (existing != null && existing.versionCode >= extension.versionCode) {
                logcat {
                    "Extension ${extension.pkgName} version ${extension.versionCode} " +
                            "not newer than ${existing.versionCode}, skipping"
                }
                return
            }
            logcat { "Registering updated extension ${extension.pkgName}" }
            registerUpdatedExtension(extension.withUpdateCheck())
            _failedExtensionsFlow.value =
                _failedExtensionsFlow.value.filterNot { it.pkgName == extension.pkgName }
            updatePendingUpdatesCount()
        }

        override fun onPackageUninstalled(pkgName: String) {
            unregisterExtension(pkgName)
            _failedExtensionsFlow.value =
                _failedExtensionsFlow.value.filterNot { it.pkgName == pkgName }
            // Don't keep quarantining a package that no longer exists.
            ExtensionLoadGuard.clearQuarantine(context, pkgName)
            updatePendingUpdatesCount()
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
        val pendingUpdateCount = _installedExtensionsFlow.value.count { it.hasUpdate }
        // Reached from the install listener, which runs on Dispatchers.Main.
        scope.launch {
            dataStore.editPreference(pendingUpdateCount, SourcesPreferences.EXT_UPDATES_COUNT)
        }
        if (pendingUpdateCount == 0) {
            ExtensionInstallerNotifier(context).dismiss()
        }
    }
}