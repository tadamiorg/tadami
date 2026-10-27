package com.sf.tadami.utils

import com.sf.tadami.storage.StorageFile
import com.sf.tadami.storage.StorageFiles
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

object DiskUtil {

    /** The root folders of the available storages, see [StorageFiles.storageRoots]. */
    fun getExternalStorages(): List<File> = Injekt.get<StorageFiles>().storageRoots()

    fun getDirectorySize(f: File): Long {
        var size: Long = 0
        if (f.isDirectory) {
            for (file in f.listFiles().orEmpty()) {
                size += getDirectorySize(file)
            }
        } else {
            size = f.length()
        }
        return size
    }

    /**
     * Gets the total space for the disk that a file path points to, in bytes.
     */
    fun getTotalStorageSpace(file: File): Long {
        return try {
            file.totalSpace
        } catch (_: Exception) {
            -1L
        }
    }

    /**
     * Gets the available space for the disk that a file path points to, in bytes.
     */
    fun getAvailableStorageSpace(file: File): Long {
        return try {
            file.usableSpace
        } catch (_: Exception) {
            -1L
        }
    }

    /**
     * Gets the available space for the disk that a file path points to, in bytes. A folder picked
     * with the storage access framework is measured on its volume. -1 when unknown.
     */
    fun getAvailableStorageSpace(f: StorageFile): Long = Injekt.get<StorageFiles>().availableSpace(f)

    /**
     * Don't display downloaded episodes in gallery apps creating `.nomedia`.
     */
    fun createNoMediaFile(dir: StorageFile?) {
        if (dir != null && dir.exists()) {
            val nomedia = dir.findFile(NOMEDIA_FILE)
            if (nomedia == null) {
                dir.createFile(NOMEDIA_FILE)
                Injekt.get<StorageFiles>().scanMedia(dir)
            }
        }
    }

    /**
     * Mutate the given filename to make it valid for a FAT filesystem,
     * replacing any invalid characters with "_". This method doesn't allow hidden files (starting
     * with a dot), but you can manually add it later.
     */
    fun buildValidFilename(origName: String): String {
        val name = origName.trim('.', ' ')
        if (name.isEmpty()) {
            return "(invalid)"
        }
        val sb = StringBuilder(name.length)
        name.forEach { c ->
            if (isValidFatFilenameChar(c)) {
                sb.append(c)
            } else {
                sb.append('_')
            }
        }
        // Even though vfat allows 255 UCS-2 chars, we might eventually write to
        // ext4 through a FUSE layer, so use that limit minus 15 reserved characters.
        return sb.toString().take(240)
    }

    /**
     * Returns true if the given character is a valid filename character, false otherwise.
     */
    private fun isValidFatFilenameChar(c: Char): Boolean {
        if (0x00.toChar() <= c && c <= 0x1f.toChar()) {
            return false
        }
        return when (c) {
            '"', '*', '/', ':', '<', '>', '?', '\\', '|', 0x7f.toChar() -> false
            else -> true
        }
    }

    const val NOMEDIA_FILE = ".nomedia"

    // Safe theoretical max filename size is 255 bytes and 1 char = 2-4 bytes (UTF-8)
    const val MAX_FILE_NAME_BYTES = 250
}
