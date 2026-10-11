package com.sf.tadami.network.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.ByteArrayOutputStream

/**
 * Hands the player HLS playlists fixed by [HlsSegmentIvs], every other response untouched.
 * Playlists are small, they are read whole when opened.
 */
@UnstableApi
class HlsPlaylistFixingDataSource(private val upstream: DataSource) : DataSource {

    private var playlist: ByteArray? = null
    private var readPosition = 0

    override fun open(dataSpec: DataSpec): Long {
        val length = upstream.open(dataSpec)
        if (dataSpec.position != 0L || length > MAX_PLAYLIST_BYTES || !isPlaylist(dataSpec.uri)) {
            return length
        }
        val original = readUpstream()
        val fixed = HlsSegmentIvs.fix(original.decodeToString())?.encodeToByteArray() ?: original
        playlist = fixed
        readPosition = 0
        return fixed.size.toLong()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val bytes = playlist ?: return upstream.read(buffer, offset, length)
        if (length == 0) return 0
        if (readPosition >= bytes.size) return C.RESULT_END_OF_INPUT
        val count = minOf(length, bytes.size - readPosition)
        System.arraycopy(bytes, readPosition, buffer, offset, count)
        readPosition += count
        return count
    }

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() {
        playlist = null
        upstream.close()
    }

    // From the network or from the cache, which keeps no headers: the URL tells, or the type does
    private fun isPlaylist(uri: Uri): Boolean {
        val path = uri.path.orEmpty().lowercase()
        if (path.endsWith(".m3u8") || path.endsWith(".m3u")) return true
        return upstream.responseHeaders.entries
            .firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }
            ?.value.orEmpty()
            .any { "mpegurl" in it.lowercase() }
    }

    private fun readUpstream(): ByteArray {
        val output = ByteArrayOutputStream()
        val chunk = ByteArray(16 * 1024)
        while (true) {
            val read = upstream.read(chunk, 0, chunk.size)
            if (read == C.RESULT_END_OF_INPUT) break
            output.write(chunk, 0, read)
        }
        return output.toByteArray()
    }

    class Factory(private val upstreamFactory: DataSource.Factory) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return HlsPlaylistFixingDataSource(upstreamFactory.createDataSource())
        }
    }

    private companion object {
        const val MAX_PLAYLIST_BYTES = 8L * 1024 * 1024
    }
}
