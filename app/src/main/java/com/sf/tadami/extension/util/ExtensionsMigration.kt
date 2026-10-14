package com.sf.tadami.extension.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.sf.tadami.preferences.model.CustomPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * Copies the extensions installed as apps, as they were before [ExtensionsStorage], into it. Their
 * sources keep their settings, which are in the app's own data. The apps themselves are never loaded.
 */
internal object ExtensionsMigration {

    // Private, so not restored: the backup of a device that was migrated doesn't migrate another one
    private val PACKAGES_MIGRATED = booleanPreferencesKey(CustomPreferences.privateKey("extension_packages_migrated"))

    /**
     * Copies each installed extension app that has no private copy, or a newer version than its copy.
     * Runs at each launch until no extension app is left, so that a failed copy is retried.
     *
     * @return The extension apps that have a private copy, so aren't needed anymore.
     */
    suspend fun migrate(context: Context, dataStore: DataStore<Preferences>): List<String> {
        if (dataStore.data.first()[PACKAGES_MIGRATED] == true) return emptyList()

        val packages = installedExtensionPackages(context)
        if (packages.isEmpty()) {
            dataStore.edit { it[PACKAGES_MIGRATED] = true }
            return emptyList()
        }

        val installer = ExtensionsInstaller(context)
        packages.forEach { pkgInfo ->
            try {
                adopt(installer, pkgInfo)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d("ExtensionsMigration", "Couldn't copy ${pkgInfo.packageName}: ${e.stackTraceToString()}")
            }
        }
        return legacyPackages(context, packages)
    }

    /** The extension apps that have a private copy, so aren't needed anymore. */
    fun legacyPackages(context: Context, packages: List<PackageInfo> = installedExtensionPackages(context)): List<String> {
        val dir = ExtensionsStorage.dir(context)
        return packages
            .map { it.packageName }
            .filter { ExtensionsStorage.isValidPkgName(it) && ExtensionsStorage.apkFile(dir, it).isFile }
    }

    /** Asks the system to uninstall the app [pkgName]: the user confirms it. */
    fun requestUninstall(context: Context, pkgName: String) {
        val intent = Intent(Intent.ACTION_DELETE, "package:$pkgName".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Whether an app of [installedVersionCode] is copied over a private copy of [privateVersionCode]. */
    internal fun shouldAdopt(installedVersionCode: Long, privateVersionCode: Long?): Boolean {
        return privateVersionCode == null || installedVersionCode > privateVersionCode
    }

    private suspend fun adopt(installer: ExtensionsInstaller, pkgInfo: PackageInfo) {
        val pkgName = pkgInfo.packageName
        val sourceDir = pkgInfo.applicationInfo?.sourceDir
        if (!ExtensionsStorage.isValidPkgName(pkgName) || sourceDir == null) return

        ExtensionsStorage.withLock(pkgName) {
            val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)
            if (!shouldAdopt(versionCode, installer.installedVersionCode(pkgName))) return@withLock

            val tmp = installer.newTmpFile(pkgName)
            try {
                File(sourceDir).copyTo(tmp, overwrite = true)
                installer.verifyAndCommit(tmp, pkgName, listed = null, allowSameVersion = false)
                    ?.let { Log.d("ExtensionsMigration", "$pkgName refused: $it") }
            } finally {
                tmp.delete()
            }
        }
    }

    private fun installedExtensionPackages(context: Context): List<PackageInfo> {
        val pkgManager = context.packageManager
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pkgManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_CONFIGURATIONS.toLong()))
        } else {
            pkgManager.getInstalledPackages(PackageManager.GET_CONFIGURATIONS)
        }
        return packages.filter { ExtensionsLoader.isPackageAnExtension(it) }
    }
}
