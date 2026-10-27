package com.sf.tadami.extension.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.io.File

/** What an extension APK declares, read without running any of its code. */
class ApkPackage(
    val file: File,
    val packageName: String,
    val versionName: String?,
    val versionCode: Long,
    /** Its label, `Tadami: <name>`. */
    val label: String,
    /** The features its manifest requires. */
    val features: List<String>,
    /** The values of its manifest meta-data as text: aapt2 stores a number as one. */
    val metaData: Map<String, String>,
    /**
     * SHA-256 fingerprints of the certificates it's verified to be signed with, the current one last.
     * With a rotated key, the older ones come first.
     */
    val signatureHashes: List<String>,
    /** Whether it's signed by several signers, see ExtensionsInstaller.canReplaceSigners. */
    val multipleSigners: Boolean,
    private val iconLoader: () -> ImageBitmap?,
) {
    /** Its launcher icon, null when it has none that can be read. */
    fun loadIcon(): ImageBitmap? = runCatching(iconLoader).getOrNull()
}

/**
 * What the extension system needs from the platform the app runs on: reading the extension APKs and
 * loading their code (dex on Android, converted to JVM bytecode on the desktop), and the Android
 * extensions installed as apps. Registered in Injekt.
 */
interface ExtensionPlatform {

    /** The app's private files directory: the extensions and their guard live under it. */
    val filesDir: File

    /** Whether this process is the app's main one, which owns the extension files. */
    val isMainProcess: Boolean get() = true

    /** The package of the APK [file], null when it isn't a valid one. */
    fun readApk(file: File): ApkPackage?

    /** A class loader of the code of [apk], [parent] first: the classes of the app win. */
    fun classLoader(apk: ApkPackage, parent: ClassLoader): ClassLoader

    /** This process' ID, for the in-flight marks of ExtensionsGuard. */
    fun processId(): Int

    /**
     * Why the process [pid], dead since [markedAt], exited, an Android ApplicationExitInfo reason,
     * null when the platform can't tell, see ExtensionsGuard.checkPreviousExit.
     */
    fun processExitReason(pid: Int, markedAt: Long): Int? = null

    /**
     * Copies the extensions installed as apps before, see ExtensionsMigration.
     *
     * @return Those not needed anymore, that the user may uninstall.
     */
    suspend fun migrateLegacyPackages(dataStore: DataStore<Preferences>): List<String> = emptyList()

    /** The extensions still installed as apps that are copied, so not needed. */
    fun legacyPackages(): List<String> = emptyList()

    /** Asks to uninstall the extension app [pkgName]. */
    fun requestUninstallLegacyPackage(pkgName: String) = Unit

    /** Debug builds: calls [onAdopted] with each extension copied once installed with adb. */
    fun watchDebugPackages(onAdopted: suspend (pkgName: String) -> Unit) = Unit

    /** Tells the user the extensions [names] have an update. */
    fun promptUpdates(names: List<String>) = Unit

    /** No update of the extensions is pending anymore: removes its notification. */
    fun dismissUpdatesNotification() = Unit
}
