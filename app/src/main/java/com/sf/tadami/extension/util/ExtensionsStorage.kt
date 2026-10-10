package com.sf.tadami.extension.util

import java.io.File
import java.io.IOException
import android.content.Context

/**
 * Owns the private directory extension APKs live in now that they are no longer installed as
 * packages.
 *
 * Two constraints shape everything here:
 *  - The files must sit on **internal** storage. ART refuses to load code from anywhere the app
 *    doesn't exclusively own, which rules out the `getExternalFilesDir` location the old
 *    DownloadManager flow used.
 *  - A file must be **read-only** before any class loader touches it. Since Android 14 a writable
 *    dex is a `SecurityException`, on the reasoning that code you can still modify after loading is
 *    code an attacker can modify too.
 */
internal object ExtensionsStorage {

    private const val DIR_NAME = "extensions"
    private const val APK_SUFFIX = ".apk"
    private const val TMP_SUFFIX = ".tmp"

    fun dir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    fun apkFile(context: Context, pkgName: String): File =
        File(dir(context), pkgName + APK_SUFFIX)

    fun tmpFile(context: Context, pkgName: String): File =
        File(dir(context), pkgName + APK_SUFFIX + TMP_SUFFIX)

    /** Every extension currently on disk. The package name is the file name. */
    fun installed(context: Context): List<File> =
        dir(context)
            .listFiles { file -> file.isFile && file.name.endsWith(APK_SUFFIX) }
            ?.toList()
            .orEmpty()

    fun pkgNameOf(file: File): String = file.name.removeSuffix(APK_SUFFIX)

    fun exists(context: Context, pkgName: String): Boolean = apkFile(context, pkgName).exists()

    /**
     * Moves a verified download into place and seals it.
     *
     * The rename is what makes an install atomic: a process death mid-download leaves a `.tmp`
     * behind, never a half-written extension that the next launch would try to class-load.
     */
    fun commit(context: Context, pkgName: String, tmp: File): File {
        val target = apkFile(context, pkgName)
        // An extension already in place is read-only, so it can't just be overwritten.
        if (target.exists() && !delete(context, pkgName)) {
            throw IOException("Could not replace the existing $pkgName extension")
        }
        if (!tmp.renameTo(target)) {
            throw IOException("Could not move $tmp into place")
        }
        target.setReadOnly()
        return target
    }

    /** Uninstalling is now just this. */
    fun delete(context: Context, pkgName: String): Boolean {
        val file = apkFile(context, pkgName)
        if (!file.exists()) return false
        // Undo commit()'s seal, otherwise the delete fails on the same platforms that need it.
        file.setWritable(true, true)
        return file.delete()
    }

    /** Drops leftovers from downloads that were interrupted before [commit]. */
    fun cleanTmp(context: Context) {
        dir(context)
            .listFiles { file -> file.name.endsWith(TMP_SUFFIX) }
            ?.forEach { it.delete() }
    }
}
