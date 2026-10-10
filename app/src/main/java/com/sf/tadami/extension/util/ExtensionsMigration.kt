package com.sf.tadami.extension.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.edit
import androidx.core.net.toUri
import com.sf.tadami.utils.logcat
import logcat.LogPriority
import java.io.File

/**
 * Moves extensions that were installed as Android packages into [ExtensionsStorage].
 *
 * Users upgrading from a build that installed extensions have them sitting in the system's package
 * list, where nothing looks for them any more. Rather than making everyone reinstall, this copies
 * each installed extension's APK — which is world-readable — into our private directory, so their
 * sources and per-source settings simply keep working across the update.
 *
 * This is the **only** remaining reason the app holds `QUERY_ALL_PACKAGES`; both can be dropped one
 * release after this ships.
 */
internal object ExtensionsMigration {

    private const val PREFS_NAME = "extension_migration"
    private const val KEY_DONE = "packages_migrated"
    private const val EXTENSION_FEATURE = "tadami.extension"

    /**
     * Runs the migration if it hasn't run before, and returns the packages that were adopted so
     * the caller can tell the user those apps are now redundant.
     *
     * Safe to call on every launch: it no-ops after the first success.
     */
    fun migrateInstalledPackages(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DONE, false)) return emptyList()

        val result = runCatching { copyInstalledExtensions(context) }
            .onFailure { logcat(LogPriority.ERROR, it) { "Extension migration failed" } }

        // Only close the door once the package list was actually read. If enumeration itself blew
        // up, retrying next launch is the difference between the user keeping their sources and
        // silently losing all of them.
        if (result.isSuccess) {
            // Marked done even when nothing was found: a fresh install has nothing to migrate and
            // shouldn't pay for a package enumeration on every launch forever.
            prefs.edit(commit = true) { putBoolean(KEY_DONE, true) }
        }

        val migrated = result.getOrDefault(emptyList())
        if (migrated.isNotEmpty()) {
            logcat(LogPriority.INFO) { "Adopted ${migrated.size} previously installed extension(s)" }
        }
        return migrated
    }

    /**
     * Extension packages that are still installed but whose APK we already hold in
     * [ExtensionsStorage]. Removing one of these cannot cost the user a source, which is the whole
     * safety property here: an extension we failed to adopt is deliberately never listed.
     */
    fun redundantPackages(context: Context): List<String> = runCatching {
        installedExtensionPackages(context)
            .map { it.packageName }
            .filter { ExtensionsStorage.exists(context, it) }
    }.onFailure {
        logcat(LogPriority.ERROR, it) { "Couldn't list redundant extension packages" }
    }.getOrDefault(emptyList())

    /**
     * Asks the system to uninstall a redundant extension package.
     *
     * A request, not an action: Android always confirms an uninstall with the user and gives a
     * third-party app no way to skip that dialog. The loaded extension is unaffected either way —
     * it runs from our own copy, not from the package.
     */
    fun requestUninstall(context: Context, pkgName: String) {
        val intent = Intent(Intent.ACTION_DELETE, "package:$pkgName".toUri())
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun installedExtensionPackages(context: Context): List<PackageInfo> {
        val flags = PackageManager.GET_CONFIGURATIONS

        val installedPkgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getInstalledPackages(
                PackageManager.PackageInfoFlags.of(flags.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstalledPackages(flags)
        }

        return installedPkgs.filter { pkgInfo ->
            pkgInfo.reqFeatures.orEmpty().any { it.name == EXTENSION_FEATURE }
        }
    }

    private fun copyInstalledExtensions(context: Context): List<String> {
        return installedExtensionPackages(context)
            .asSequence()
            .filterNot { ExtensionsStorage.exists(context, it.packageName) }
            .mapNotNull { pkgInfo ->
                val pkgName = pkgInfo.packageName
                val sourceDir = pkgInfo.applicationInfo?.sourceDir
                if (sourceDir == null) {
                    logcat(LogPriority.WARN) { "No APK path for installed extension $pkgName" }
                    return@mapNotNull null
                }
                runCatching { adopt(context, pkgName, File(sourceDir)) }
                    .onFailure { logcat(LogPriority.ERROR, it) { "Couldn't adopt $pkgName" } }
                    .map { pkgName }
                    .getOrNull()
            }
            .toList()
    }

    private fun adopt(context: Context, pkgName: String, installedApk: File) {
        val tmp = ExtensionsStorage.tmpFile(context, pkgName)
        try {
            installedApk.inputStream().use { input ->
                tmp.outputStream().use { output -> input.copyTo(output) }
            }
            ExtensionsStorage.commit(context, pkgName, tmp)
        } finally {
            tmp.delete()
        }
    }
}
