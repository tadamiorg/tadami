package com.sf.tadami.extension.util

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * The installed extensions: one APK per extension, `filesDir/extensions/<pkgName>.apk`, read-only as
 * Android 14+ refuses to load code from a writable file.
 */
internal object ExtensionsStorage {

    private const val DIR_NAME = "extensions"
    private const val APK_SUFFIX = ".apk"
    private const val TMP_SUFFIX = ".tmp"

    /** Where ART puts what it compiles of an APK of the folder: `oat/<isa>/<pkgName>.odex`... */
    private const val OAT_DIR_NAME = "oat"

    // Dot separated names of letters, digits and underscores: nothing that leads out of the folder
    private val PKG_NAME_REGEX = Regex("^[A-Za-z]\\w*(\\.[A-Za-z]\\w*)+$")

    private val locks = ConcurrentHashMap<String, Mutex>()

    fun isValidPkgName(pkgName: String): Boolean = PKG_NAME_REGEX.matches(pkgName)

    fun dir(context: Context): File = File(context.filesDir, DIR_NAME)

    fun apkFile(dir: File, pkgName: String): File {
        require(isValidPkgName(pkgName)) { "Invalid package name $pkgName" }
        return File(dir, pkgName + APK_SUFFIX)
    }

    /** The package names of the extensions in [dir]. */
    fun pkgNames(dir: File): List<String> {
        return dir.list().orEmpty()
            .filter { it.endsWith(APK_SUFFIX) }
            .map { it.removeSuffix(APK_SUFFIX) }
            .filter { isValidPkgName(it) }
    }

    /** A new file of [dir] to write an APK of [pkgName] into, before its [commit]. */
    fun newTmpFile(dir: File, pkgName: String): File {
        require(isValidPkgName(pkgName)) { "Invalid package name $pkgName" }
        dir.mkdirs()
        return File.createTempFile("$pkgName-", TMP_SUFFIX, dir)
    }

    /**
     * Makes [tmp] the APK of [pkgName]. It replaces the current one atomically: a process death
     * leaves one or the other, never none nor a partial one.
     */
    fun commit(dir: File, pkgName: String, tmp: File): File {
        val target = apkFile(dir, pkgName)
        // The rename replaces the read-only target as well, only the folder has to be writable
        if (!tmp.setReadOnly() || !tmp.renameTo(target)) {
            throw IOException("Couldn't move $tmp to $target")
        }
        return target
    }

    /** Deletes the APK of [pkgName] and what ART compiled of it. */
    fun delete(dir: File, pkgName: String): Boolean {
        val deleted = apkFile(dir, pkgName).delete()
        val oatFiles = File(dir, OAT_DIR_NAME).listFiles().orEmpty()
        oatFiles
            .flatMap { it.listFiles().orEmpty().asList() }
            .filter { it.nameWithoutExtension == pkgName }
            .forEach { it.delete() }
        // The profiles ART records of it, next to the ISA folders: `oat/<pkgName>.apk.cur.prof`...
        oatFiles
            .filter { it.isFile && it.name.startsWith(pkgName + APK_SUFFIX + ".") }
            .forEach { it.delete() }
        return deleted
    }

    /**
     * Deletes what ART compiled or recorded of the extensions whose APK isn't in [dir] anymore: it
     * can write the profile of one again a while after its deletion, see [delete].
     */
    fun cleanOrphanOatFiles(dir: File) {
        File(dir, OAT_DIR_NAME).walkBottomUp()
            .filter { it.isFile }
            .forEach { file ->
                val pkgName = oatFilePkgName(file.name)
                if (isValidPkgName(pkgName) && !apkFile(dir, pkgName).exists()) file.delete()
            }
    }

    /** The package name of an ART file: `<pkgName>.odex`, `<pkgName>.apk.cur.prof`... */
    private fun oatFilePkgName(fileName: String): String {
        val apkPrefix = "$APK_SUFFIX."
        return if (apkPrefix in fileName) fileName.substringBeforeLast(apkPrefix) else fileName.substringBeforeLast('.')
    }

    /** Deletes the files of the downloads and copies a process death stopped. */
    fun cleanTmp(dir: File) {
        dir.listFiles { file -> file.name.endsWith(TMP_SUFFIX) }?.forEach { it.delete() }
    }

    /** Runs [block] while no other install, update or deletion of [pkgName] runs. */
    suspend fun <T> withLock(pkgName: String, block: suspend () -> T): T {
        return locks.getOrPut(pkgName) { Mutex() }.withLock { block() }
    }
}
