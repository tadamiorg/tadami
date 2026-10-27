package com.sf.tadami.storage

import android.app.Application
import android.os.ParcelFileDescriptor
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** A storage access framework document, or a plain file, through UniFile. */
class AndroidStorageFile(
    val uniFile: UniFile,
    private val app: Application,
) : StorageFile() {

    override val name: String? get() = uniFile.name

    override val uri: String get() = uniFile.uri.toString()

    override val filePath: String? get() = uniFile.filePath

    override val type: String? get() = uniFile.type

    override val parentFile: StorageFile? get() = uniFile.parentFile?.let { AndroidStorageFile(it, app) }

    override val isDirectory: Boolean get() = uniFile.isDirectory

    override val isFile: Boolean get() = uniFile.isFile

    override fun exists(): Boolean = uniFile.exists()

    override fun length(): Long = uniFile.length()

    override fun lastModified(): Long = uniFile.lastModified()

    override fun listFiles(): Array<StorageFile>? = uniFile.listFiles()?.map { AndroidStorageFile(it, app) }?.toTypedArray()

    override fun findFile(displayName: String): StorageFile? = uniFile.findFile(displayName)?.let { AndroidStorageFile(it, app) }

    override fun createDirectory(displayName: String): StorageFile? =
        uniFile.createDirectory(displayName)?.let { AndroidStorageFile(it, app) }

    override fun createFile(displayName: String): StorageFile? = uniFile.createFile(displayName)?.let { AndroidStorageFile(it, app) }

    override fun delete(): Boolean = uniFile.delete()

    override fun renameTo(displayName: String): Boolean = uniFile.renameTo(displayName)

    override fun openInputStream(): InputStream = uniFile.openInputStream()

    override fun openOutputStream(append: Boolean): OutputStream = uniFile.openOutputStream(append)

    override fun openOutputAt(position: Long): OutputStream {
        val descriptor = app.contentResolver.openFileDescriptor(uniFile.uri, "rw")
            ?: throw IOException("Can't open $name")
        val stream = ParcelFileDescriptor.AutoCloseOutputStream(descriptor)
        try {
            stream.channel.truncate(position)
            stream.channel.position(position)
        } catch (e: IOException) {
            stream.close()
            throw e
        }
        return stream
    }
}

class AndroidStorageFiles(private val app: Application) : StorageFiles {

    override fun fromUri(uri: String): StorageFile? = UniFile.fromUri(app, uri.toUri())?.let { AndroidStorageFile(it, app) }

    override fun fromFile(file: File): StorageFile = AndroidStorageFile(UniFile.fromFile(file)!!, app)
}

/** The UniFile of this storage file, for the Android APIs. */
val StorageFile.uniFile: UniFile get() = (this as AndroidStorageFile).uniFile
