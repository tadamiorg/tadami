package com.sf.tadami.storage

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.URI
import java.nio.channels.Channels
import java.nio.file.Files

/** A plain file of the file system. */
class DesktopStorageFile(val file: File) : StorageFile() {

    override val name: String? get() = file.name

    override val uri: String get() = file.toURI().toString()

    override val filePath: String get() = file.absolutePath

    override val type: String? get() = if (file.isDirectory) null else runCatching { Files.probeContentType(file.toPath()) }.getOrNull()

    override val parentFile: StorageFile? get() = file.parentFile?.let { DesktopStorageFile(it) }

    override val isDirectory: Boolean get() = file.isDirectory

    override val isFile: Boolean get() = file.isFile

    override fun exists(): Boolean = file.exists()

    override fun length(): Long = file.length()

    override fun lastModified(): Long = file.lastModified()

    override fun listFiles(): Array<StorageFile>? = file.listFiles()?.map { DesktopStorageFile(it) }?.toTypedArray()

    override fun findFile(displayName: String): StorageFile? = File(file, displayName).takeIf { it.exists() }?.let { DesktopStorageFile(it) }

    override fun createDirectory(displayName: String): StorageFile? =
        File(file, displayName).takeIf { it.isDirectory || it.mkdirs() }?.let { DesktopStorageFile(it) }

    override fun createFile(displayName: String): StorageFile? =
        File(file, displayName).takeIf { it.exists() || it.createNewFile() }?.let { DesktopStorageFile(it) }

    override fun delete(): Boolean = if (file.isDirectory) file.deleteRecursively() else file.delete()

    override fun renameTo(displayName: String): Boolean = file.renameTo(File(file.parentFile, displayName))

    override fun openInputStream(): InputStream = file.inputStream()

    override fun openOutputStream(append: Boolean): OutputStream = FileOutputStream(file, append)

    override fun openOutputAt(position: Long): OutputStream {
        val access = RandomAccessFile(file, "rw")
        access.setLength(position)
        access.seek(position)
        return Channels.newOutputStream(access.channel)
    }
}

class DesktopStorageFiles : StorageFiles {

    override fun fromUri(uri: String): StorageFile? = runCatching {
        if (uri.startsWith("file:")) DesktopStorageFile(File(URI(uri))) else DesktopStorageFile(File(uri))
    }.getOrNull()

    override fun fromFile(file: File): StorageFile = DesktopStorageFile(file)
}
