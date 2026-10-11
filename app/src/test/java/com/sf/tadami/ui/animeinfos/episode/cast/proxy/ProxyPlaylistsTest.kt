package com.sf.tadami.ui.animeinfos.episode.cast.proxy

import okhttp3.Headers.Companion.headersOf
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxyPlaylistsTest {

    private val playlistUrl = "https://vault-99.owocdn.top/stream/99/02/abc/uwu.m3u8".toHttpUrl()
    private val segmentUrl = "https://vault-99.owocdn.top/stream/99/02/abc/segment-3-v1-a1.jpg".toHttpUrl()

    @Test
    fun `playlist urls are recognised by their extension, query ignored`() {
        assertTrue(isPlaylistUrl(playlistUrl))
        assertTrue(isPlaylistUrl("https://cdn.example/live/INDEX.M3U8?token=1".toHttpUrl()))
        assertTrue(isPlaylistUrl("https://cdn.example/radio.m3u".toHttpUrl()))
        assertFalse(isPlaylistUrl(segmentUrl))
        assertFalse(isPlaylistUrl("https://cdn.example/video.mp4?name=a.m3u8".toHttpUrl()))
    }

    @Test
    fun `accept-encoding is dropped so okhttp decodes the playlist`() {
        val headers = withoutAcceptEncoding(headersOf("Accept-Encoding", "gzip, br", "Referer", "https://kwik.cx/"))
        assertNull(headers["Accept-Encoding"])
        assertEquals("https://kwik.cx/", headers["Referer"])
    }

    @Test
    fun `only whole decoded bodies are read`() {
        fun whole(
            method: String = "GET",
            range: Boolean = false,
            code: Int = 200,
            encoding: String? = null,
            length: Long = 23_138,
        ) = isWholeDecodedBody(method, range, code, encoding, length)

        assertTrue(whole())
        assertTrue(whole(encoding = "identity", length = -1))
        assertFalse(whole(method = "HEAD"))
        assertFalse(whole(range = true))
        assertFalse(whole(code = 403))
        assertFalse(whole(code = 206))
        assertFalse(whole(encoding = "gzip"))
        assertFalse(whole(length = MAX_FIXED_PLAYLIST_BYTES + 1))
    }

    @Test
    fun `playlists are recognised by their first bytes`() {
        assertTrue(startsLikePlaylist("#EXTM3U\n#EXT".toByteArray()))
        assertTrue(startsLikePlaylist(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "#EXTM3U".toByteArray()))
        // TS segment, empty body, truncated magic
        assertFalse(startsLikePlaylist(byteArrayOf(0x47, 0x40, 0x00, 0x10)))
        assertFalse(startsLikePlaylist(ByteArray(0)))
        assertFalse(startsLikePlaylist("#EXTM".toByteArray()))
        assertFalse(startsLikePlaylist("<html>#EXTM3U".toByteArray()))
    }

    @Test
    fun `validators of the original body are not forwarded with a rewritten one`() {
        val forwarded = listOf("ETag" to "\"1\"", "Cache-Control" to "max-age=60", "Content-Encoding" to "gzip")
        assertEquals(listOf("Cache-Control" to "max-age=60"), playlistResponseHeaders(forwarded, rewritten = true))
        assertEquals(forwarded, playlistResponseHeaders(forwarded, rewritten = false))
    }
}
