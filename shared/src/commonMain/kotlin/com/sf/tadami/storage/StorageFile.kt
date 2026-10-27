package com.sf.tadami.storage

import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * A file or folder of the storage the user picked for the downloads and backups: a storage access
 * framework document on Android (UniFile), a plain file on the desktop.
 */
abstract class StorageFile {

    abstract val name: String?

    /** Where it is: a content:// URI on Android, a file:// one on the desktop. */
    abstract val uri: String

    /** Its path in the file system, null when it has none. */
    abstract val filePath: String?

    /** Its MIME type, null for a folder or when it's unknown. */
    abstract val type: String?

    abstract val parentFile: StorageFile?

    abstract val isDirectory: Boolean

    abstract val isFile: Boolean

    abstract fun exists(): Boolean

    abstract fun length(): Long

    abstract fun lastModified(): Long

    /** Its children, null when it isn't a folder. */
    abstract fun listFiles(): Array<StorageFile>?

    abstract fun findFile(displayName: String): StorageFile?

    abstract fun createDirectory(displayName: String): StorageFile?

    abstract fun createFile(displayName: String): StorageFile?

    abstract fun delete(): Boolean

    abstract fun renameTo(displayName: String): Boolean

    abstract fun openInputStream(): InputStream

    abstract fun openOutputStream(append: Boolean = false): OutputStream

    /** Writes into it from [position], what's after it cut, to resume a download. */
    abstract fun openOutputAt(position: Long): OutputStream

    override fun equals(other: Any?): Boolean = other is StorageFile && other.uri == uri

    override fun hashCode(): Int = uri.hashCode()

    override fun toString(): String = uri
}

/** The storage files of a platform, registered in Injekt. */
interface StorageFiles {

    /** The file or folder at [uri], null when it can't be reached. */
    fun fromUri(uri: String): StorageFile?

    fun fromFile(file: File): StorageFile

    /** Reads the file at [uri], of any app on Android: [fromUri] only reaches the documents and media. */
    fun openInputStream(uri: String): InputStream? = fromUri(uri)?.openInputStream()

    /** The available space of the volume of [dir], in bytes, -1 when unknown. */
    fun availableSpace(dir: StorageFile): Long = dir.filePath?.let { File(it).usableSpace } ?: -1L

    /** Has the gallery apps see what changed in [dir]. */
    fun scanMedia(dir: StorageFile) = Unit

    /** The root folders of the storages the downloads can go to. */
    fun storageRoots(): List<File> = File.listRoots().toList()
}
