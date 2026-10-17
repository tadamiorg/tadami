package com.sf.tadami.data.download.stream

import android.content.Context
import android.os.ParcelFileDescriptor
import com.hippo.unifile.UniFile
import java.io.IOException
import java.io.OutputStream

/** Writing to the storage folder, where any failure stops the download, see [StorageException]. */
object DownloadOutput {

    /**
     * [file] opened for writing at [position], with everything after it cut: a resume overwrites
     * the half written end. "w" doesn't truncate for every provider, "rw" and an explicit cut do.
     */
    fun openAt(context: Context, file: UniFile, position: Long): OutputStream {
        return storage {
            val descriptor = context.contentResolver.openFileDescriptor(file.uri, "rw")
                ?: throw IOException("Can't open ${file.name}")
            val stream = ParcelFileDescriptor.AutoCloseOutputStream(descriptor)
            try {
                stream.channel.truncate(position)
                stream.channel.position(position)
            } catch (e: IOException) {
                stream.close()
                throw e
            }
            stream
        }
    }

    /** The file named [name] in [dir], created when missing. */
    fun getOrCreateFile(dir: UniFile, name: String): UniFile {
        return storage {
            dir.findFile(name) ?: dir.createFile(name) ?: throw IOException("Can't create $name")
        }
    }

    fun write(output: OutputStream, data: ByteArray, offset: Int = 0, length: Int = data.size) {
        storage { output.write(data, offset, length) }
    }

    inline fun <T> storage(block: () -> T): T {
        return try {
            block()
        } catch (e: IOException) {
            throw StorageException(e)
        } catch (e: SecurityException) {
            // The folder permission was revoked
            throw StorageException(e)
        }
    }
}
