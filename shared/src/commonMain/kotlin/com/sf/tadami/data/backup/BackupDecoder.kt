package com.sf.tadami.data.backup

import com.sf.tadami.storage.StorageFiles
import com.sf.tadami.utils.stringRes
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.data.backup.models.Backup
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.protobuf.ProtoBuf
import okio.buffer
import okio.gzip
import okio.source
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.IOException

class BackupDecoder @OptIn(ExperimentalSerializationApi::class) constructor(
    private val parser: ProtoBuf = Injekt.get(),
) {
    /**
     * Decode a potentially-gzipped backup.
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun decode(uri: String): Backup {
        return Injekt.get<StorageFiles>().openInputStream(uri)!!.use { inputStream ->
            val source = inputStream.source().buffer()

            val peeked = source.peek().apply {
                require(2)
            }
            val id1id2 = peeked.readShort()
            val backupString = when (id1id2.toInt()) {
                0x1f8b -> source.gzip().buffer() // 0x1f8b is gzip magic bytes
                MAGIC_JSON_SIGNATURE1, MAGIC_JSON_SIGNATURE2, MAGIC_JSON_SIGNATURE3 -> {
                    throw IOException(stringRes(Res.string.restoring_backup_error))
                }
                else -> source
            }.use { it.readByteArray() }

            try {
                parser.decodeFromByteArray(Backup.serializer(), backupString)
            } catch (_: SerializationException) {
                throw IOException(stringRes(Res.string.unknown_error))
            }
        }
    }

    companion object {
        private const val MAGIC_JSON_SIGNATURE1 = 0x7b7d // `{}`
        private const val MAGIC_JSON_SIGNATURE2 = 0x7b22 // `{"`
        private const val MAGIC_JSON_SIGNATURE3 = 0x7b0a // `{\n`
    }
}
