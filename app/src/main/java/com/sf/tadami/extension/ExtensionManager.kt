package com.sf.tadami.extension

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
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
import com.sf.tadami.notifications.extensionsinstaller.ExtensionInstallerNotifier
import com.sf.tadami.notifications.extensionsinstaller.ExtensionInstallerReceiver
import com.sf.tadami.preferences.extensions.TrustedExtensionsPreferences
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.source.StubSource
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.utils.editPreference
import com.sf.tadami.utils.getPreferencesGroup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
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
     * The installer which installs, updates and uninstalls the extensions.
     */
    private val installer by lazy { ExtensionsInstaller(context) }
    private val iconMap = ConcurrentHashMap<String, Drawable>()

    private val _installedExtensionsFlow = MutableStateFlow(emptyList<Extension.Installed>())
    val installedExtensionsFlow = _installedExtensionsFlow.asStateFlow()

    fun getAppIconForSource(sourceId: Long): Drawable? {
        val extension = _installedExtensionsFlow.value
            .find { ext ->
                ext.sources.any { it.id == sourceId }
            }
            ?: return null

        // Prefer the icon already loaded with the extension
        extension.icon?.let { return it }

        val pkgName = extension.pkgName
        iconMap[pkgName]?.let { return it }
        // The package may have just been uninstalled without PACKAGE_REMOVED being processed yet
        val icon = ExtensionsLoader.getExtensionPackageInfoFromPkgName(context, pkgName)
            ?.applicationInfo
            ?.loadIcon(context.packageManager)
            ?: return null
        iconMap[pkgName] = icon
        return icon
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
        ExtensionInstallerReceiver(InstallationListener()).register(context)
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
        val extensions = ExtensionsLoader.loadExtensions(context)

        _installedExtensionsFlow.value = extensions
            .filterIsInstance<LoadResult.Success>()
            .map { it.extension }

        isInitialized = true
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
        installer.uninstallApk(extension.pkgName)
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
                .forEach {
                    val result = ExtensionsLoader.loadExtensionFromPkgName(context, it.pkgName)
                    if (result is LoadResult.Success) {
                        registerUpdatedExtension(result.extension.withUpdateCheck())
                    }
                }
            updatePendingUpdatesCount()
        }
    }

    /**
     * Registers the given extension in this and the source managers.
     *
     * @param extension The extension to be registered.
     */
    private fun registerNewExtension(extension: Extension.Installed) {
        _installedExtensionsFlow.update { it + extension }
    }

    /**
     * Registers the given updated extension in this and the source managers previously removing
     * the outdated ones.
     *
     * @param extension The extension to be registered.
     */
    private fun registerUpdatedExtension(extension: Extension.Installed) {
        iconMap.remove(extension.pkgName)
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
     * Unregisters the extension in this and the source managers given its package name. Note this
     * method is called for every uninstalled application in the system.
     *
     * @param pkgName The package name of the uninstalled application.
     */
    private fun unregisterExtension(pkgName: String) {
        iconMap.remove(pkgName)
        _installedExtensionsFlow.update { installedExtensions ->
            val installedExtension = installedExtensions.find { it.pkgName == pkgName }
            if (installedExtension != null) installedExtensions - installedExtension else installedExtensions
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
                Log.d("ExtensionManager", "Extension ${extension.pkgName} already loaded, skipping")
                return
            }
            Log.d("ExtensionManager", "Registering new extension ${extension.pkgName}")
            registerNewExtension(extension.withUpdateCheck())
            updatePendingUpdatesCount()
        }

        override fun onExtensionUpdated(extension: Extension.Installed) {
            Log.d("ExtensionManager", "onExtensionUpdated called for ${extension.pkgName} version ${extension.versionCode}")
            val existing = _installedExtensionsFlow.value.find { it.pkgName == extension.pkgName }
            Log.d("ExtensionManager", "Existing version: ${existing?.versionCode}")

            // Only update if version is actually newer, or if the loaded one can't be used: a
            // reinstall of the same version may fix it
            if (existing != null && existing.loadIssue == null && existing.versionCode >= extension.versionCode) {
                Log.d("ExtensionManager", "Extension ${extension.pkgName} version ${extension.versionCode} not newer than ${existing.versionCode}, skipping")
                return
            }
            Log.d("ExtensionManager", "Registering updated extension ${extension.pkgName}")
            registerUpdatedExtension(extension.withUpdateCheck())
            updatePendingUpdatesCount()
        }

        override fun onPackageUninstalled(pkgName: String) {
            // Called for any app, only an extension can change the count
            if (_installedExtensionsFlow.value.none { it.pkgName == pkgName }) return
            unregisterExtension(pkgName)
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
        // The update statuses aren't known before the available extensions are, keep the count
        // the startup check stored
        if (_availableExtensionsFlow.value.isEmpty()) return
        val pendingUpdateCount = _installedExtensionsFlow.value.count { it.hasUpdate }
        runBlocking {
            dataStore.editPreference(pendingUpdateCount,SourcesPreferences.EXT_UPDATES_COUNT)
        }
        if (pendingUpdateCount == 0) {
            ExtensionInstallerNotifier(context).dismiss()
        }
    }
}