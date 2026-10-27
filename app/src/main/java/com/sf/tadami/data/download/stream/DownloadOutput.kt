package com.sf.tadami.data.download.stream

import com.sf.tadami.storage.StorageFile
import java.io.IOException
import java.io.OutputStream

/** Writing to the storage folder, where any failure stops the download, see [StorageException]. */
object DownloadOutput {

    /**
     * [file] opened for writing at [position], with everything after it cut: a resume overwrites
     * the half written end. "w" doesn't truncate for every provider, "rw" and an explicit cut do.
     */
    fun openAt(file: StorageFile, position: Long): OutputStream {
        return storage { file.openOutputAt(position) }
    }

    /** The file named [name] in [dir], created when missing. */
    fun getOrCreateFile(dir: StorageFile, name: String): StorageFile {
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
