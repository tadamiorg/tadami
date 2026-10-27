package com.sf.tadami.storage

import android.app.Application
import android.content.ContentResolver
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.StatFs
import android.provider.DocumentsContract
import androidx.core.content.ContextCompat
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

    override fun openInputStream(uri: String): InputStream? = app.contentResolver.openInputStream(uri.toUri())

    override fun availableSpace(dir: StorageFile): Long {
        return try {
            val stat = StatFs(volumePath(dir.uri.toUri()) ?: return -1L)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Exception) {
            -1L
        }
    }

    /** A path on the volume of [uri]: its own for a file, the volume's for an external storage document. */
    private fun volumePath(uri: Uri): String? {
        if (uri.scheme == ContentResolver.SCHEME_FILE) return uri.path
        if (uri.authority != EXTERNAL_STORAGE_DOCUMENTS) return null
        val documentId = runCatching { DocumentsContract.getDocumentId(uri) }
            .recoverCatching { DocumentsContract.getTreeDocumentId(uri) }
            .getOrNull() ?: return null
        // "primary:TadamiDbg/downloads", "1A2B-3C4D:Anime"
        return when (val volume = documentId.substringBefore(':')) {
            "primary", "home" -> Environment.getExternalStorageDirectory().path
            else -> "/storage/$volume"
        }
    }

    override fun scanMedia(dir: StorageFile) {
        MediaScannerConnection.scanFile(app, arrayOf(dir.uri.toUri().path), null, null)
    }

    override fun storageRoots(): List<File> {
        return ContextCompat.getExternalFilesDirs(app, null)
            .filterNotNull()
            .mapNotNull {
                val file = File(it.absolutePath.substringBefore("/Android/"))
                val state = Environment.getExternalStorageState(file)
                if (state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY) {
                    file
                } else {
                    null
                }
            }
    }

    private companion object {
        const val EXTERNAL_STORAGE_DOCUMENTS = "com.android.externalstorage.documents"
    }
}

/** The UniFile of this storage file, for the Android APIs. */
val StorageFile.uniFile: UniFile get() = (this as AndroidStorageFile).uniFile
