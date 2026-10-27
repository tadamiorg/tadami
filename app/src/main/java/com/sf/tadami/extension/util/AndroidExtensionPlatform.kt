package com.sf.tadami.extension.util

import android.app.ActivityManager
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.notifications.extensionsinstaller.ExtensionInstallerNotifier
import dalvik.system.PathClassLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * The extensions on Android: their APKs read by the PackageManager, their dex loaded by a
 * PathClassLoader, the crashes attributed by the exit reasons of the previous processes.
 */
class AndroidExtensionPlatform(private val app: Application) : ExtensionPlatform {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val filesDir: File get() = app.filesDir

    // Not from another process, the crash screen's for instance
    override val isMainProcess: Boolean get() = Application.getProcessName() == app.packageName

    override fun readApk(file: File): ApkPackage? {
        val pkgManager = app.packageManager
        val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pkgManager.getPackageArchiveInfo(file.path, PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
        } else {
            pkgManager.getPackageArchiveInfo(file.path, PACKAGE_FLAGS)
        } ?: return null
        val appInfo = pkgInfo.applicationInfo ?: return null
        // On Android 13+ the ApplicationInfo of getPackageArchiveInfo has no sourceDir, which the
        // icon is loaded from
        if (appInfo.sourceDir == null) appInfo.sourceDir = file.path
        if (appInfo.publicSourceDir == null) appInfo.publicSourceDir = file.path
        val metaData = appInfo.metaData
        return ApkPackage(
            file = file,
            packageName = pkgInfo.packageName,
            versionName = pkgInfo.versionName,
            versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo),
            label = pkgManager.getApplicationLabel(appInfo).toString(),
            features = pkgInfo.reqFeatures.orEmpty().mapNotNull { it.name },
            // aapt2 stores a lone ID that fits in an int as an integer, so the value isn't always a string
            metaData = metaData?.keySet().orEmpty().associateWith { @Suppress("DEPRECATION") metaData?.get(it)?.toString().orEmpty() },
            signatureHashes = signatureHashes(pkgInfo),
            multipleSigners = pkgInfo.signingInfo?.hasMultipleSigners() == true,
            iconLoader = { appInfo.loadIcon(pkgManager)?.toBitmap()?.asImageBitmap() },
        )
    }

    override fun classLoader(apk: ApkPackage, parent: ClassLoader): ClassLoader =
        PathClassLoader(apk.file.path, null, parent)

    override fun processId(): Int = Process.myPid()

    override fun processExitReason(pid: Int, markedAt: Long): Int? {
        // Before Android 11 the exit reasons are unknown
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        // The last exit of that pid, if it's the death of the process that left the mark
        return app.getSystemService<ActivityManager>()
            ?.getHistoricalProcessExitReasons(app.packageName, pid, 1)
            ?.firstOrNull { it.processName == app.packageName && it.timestamp >= markedAt }
            ?.reason
    }

    override suspend fun migrateLegacyPackages(dataStore: DataStore<Preferences>): List<String> =
        ExtensionsMigration.migrate(app, dataStore)

    override fun legacyPackages(): List<String> = ExtensionsMigration.legacyPackages(app)

    override fun requestUninstallLegacyPackage(pkgName: String) = ExtensionsMigration.requestUninstall(app, pkgName)

    /** Copies an extension as soon as it's installed with adb, then gives it to [onAdopted]. */
    override fun watchDebugPackages(onAdopted: suspend (pkgName: String) -> Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // The update of an app is also given as PACKAGE_REPLACED
                if (intent.action == Intent.ACTION_PACKAGE_ADDED &&
                    intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                ) {
                    return
                }
                val pkgName = intent.data?.encodedSchemeSpecificPart ?: return
                scope.launch {
                    if (ExtensionsMigration.adoptPackage(context, pkgName)) onAdopted(pkgName)
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(app, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun promptUpdates(names: List<String>) = ExtensionInstallerNotifier(app).promptUpdates(names)

    override fun dismissUpdatesNotification() = ExtensionInstallerNotifier(app).dismiss()

    companion object {
        // GET_SIGNATURES as well: getPackageArchiveInfo of API 28 and 29 only reads the certificates
        // with it, even for signingInfo
        @Suppress("DEPRECATION")
        private const val PACKAGE_FLAGS = PackageManager.GET_CONFIGURATIONS or
            PackageManager.GET_META_DATA or
            PackageManager.GET_SIGNATURES or
            PackageManager.GET_SIGNING_CERTIFICATES

        private const val EXTENSION_FEATURE = "tadami.extension"

        /**
         * SHA-256 fingerprints of the certificates [pkgInfo] is signed with, the current one last.
         * With a rotated key, the older ones the platform verified come first.
         */
        fun signatureHashes(pkgInfo: PackageInfo): List<String> {
            // signingInfo is there from API 28, the minSdk: no need for the legacy signatures
            val signingInfo = pkgInfo.signingInfo ?: return emptyList()
            val signatures = if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
            return signatures.orEmpty().map { ExtensionsLoader.sha256(it.toByteArray()) }
        }

        /** Whether the installed app [this] is an extension. */
        fun PackageInfo.isTadamiExtension(): Boolean = reqFeatures.orEmpty().any { it.name == EXTENSION_FEATURE }
    }
}
