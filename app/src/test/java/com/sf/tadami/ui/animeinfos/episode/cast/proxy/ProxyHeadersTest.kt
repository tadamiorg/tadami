package com.sf.tadami.ui.animeinfos.episode.cast.proxy

import okhttp3.Headers.Companion.headersOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxyHeadersTest {

    private val ua = "Tadami-Test-UA/1.0"

    // --- parseHeadersParam ---

    @Test
    fun `parses a json object of string values`() {
        val headers = parseHeadersParam("""{"Referer":"https://anime-site.example/","X-Token":"abc"}""")!!
        assertEquals("https://anime-site.example/", headers["Referer"])
        assertEquals("abc", headers["X-Token"])
    }

    @Test
    fun `skips non-string values instead of failing`() {
        val headers = parseHeadersParam("""{"Referer":"https://x/","Weird":{"nested":1}}""")!!
        assertEquals("https://x/", headers["Referer"])
        assertNull(headers["Weird"])
    }

    @Test
    fun `null blank or malformed input yields null`() {
        assertNull(parseHeadersParam(null))
        assertNull(parseHeadersParam(""))
        assertNull(parseHeadersParam("  "))
        assertNull(parseHeadersParam("not json"))
        assertNull(parseHeadersParam("[1,2]"))
    }

    // --- buildUpstreamHeaders ---

    @Test
    fun `excluded incoming headers are dropped, the rest flow through`() {
        // Keys lowercased, as NanoHTTPD delivers them (incl. its pseudo-entries).
        val incoming = mapOf(
            "host" to "192.168.1.23:8214",
            "origin" to "https://receiver.example",
            "accept-language" to "fr",
            "connection" to "keep-alive",
            "content-length" to "0",
            "http-client-ip" to "192.168.1.40",
            "remote-addr" to "192.168.1.40",
            "range" to "bytes=100-",
            "accept" to "*/*",
            "accept-encoding" to "gzip, deflate",
            "if-none-match" to "\"etag\"",
        )
        val result = buildUpstreamHeaders(incoming, null, "")
        assertEquals("bytes=100-", result["Range"])
        assertEquals("*/*", result["Accept"])
        assertEquals("gzip, deflate", result["Accept-Encoding"])
        assertEquals("\"etag\"", result["If-None-Match"])
        assertNull(result["Host"])
        assertNull(result["Origin"])
        assertNull(result["Accept-Language"])
        assertNull(result["Connection"])
        assertNull(result["Content-Length"])
        assertNull(result["http-client-ip"])
        assertNull(result["remote-addr"])
    }

    @Test
    fun `carried headers overlay and win conflicts`() {
        val incoming = mapOf("x-custom" to "client-value", "range" to "bytes=0-")
        val carried = headersOf(
            "X-Custom", "source-value",
            "Referer", "https://anime-site.example/",
        )
        val result = buildUpstreamHeaders(incoming, carried, "")
        assertEquals("source-value", result["X-Custom"])
        assertEquals(1, result.values("X-Custom").size)
        assertEquals("https://anime-site.example/", result["Referer"])
        assertEquals("bytes=0-", result["Range"])
    }

    @Test
    fun `carried user agent beats the preference fallback`() {
        val carried = headersOf("User-Agent", "Source-Specific-UA")
        assertEquals("Source-Specific-UA", buildUpstreamHeaders(emptyMap(), carried, ua)["User-Agent"])
    }

    @Test
    fun `preference user agent is added only when absent`() {
        assertEquals(ua, buildUpstreamHeaders(emptyMap(), null, ua)["User-Agent"])
        assertEquals(ua, buildUpstreamHeaders(emptyMap(), headersOf("Referer", "x"), ua)["User-Agent"])
        val incomingUa = mapOf("user-agent" to "Chromecast-Chrome")
        assertEquals("Chromecast-Chrome", buildUpstreamHeaders(incomingUa, null, ua)["User-Agent"])
    }

    @Test
    fun `blank preference user agent adds nothing`() {
        assertNull(buildUpstreamHeaders(emptyMap(), null, " ")["User-Agent"])
        assertNull(buildUpstreamHeaders(emptyMap(), null, "")["User-Agent"])
    }

    // --- forwardedResponseHeaders ---

    @Test
    fun `framing and cors response headers are dropped`() {
        val upstream = headersOf(
            "Content-Type", "video/mp4",
            "Content-Length", "1234",
            "Transfer-Encoding", "chunked",
            "Connection", "keep-alive",
            "Access-Control-Allow-Origin", "https://upstream.example",
        )
        assertTrue(forwardedResponseHeaders(upstream).isEmpty())
    }

    @Test
    fun `everything else is forwarded verbatim`() {
        val upstream = headersOf(
            "Content-Range", "bytes 100-199/1000",
            "Accept-Ranges", "bytes",
            "ETag", "\"etag\"",
            "Content-Encoding", "gzip",
            "Cache-Control", "max-age=60",
            "Content-Type", "video/mp4",
        )
        val forwarded = forwardedResponseHeaders(upstream)
        val names = forwarded.map { it.first }
        assertTrue("Content-Range" in names)
        assertTrue("Accept-Ranges" in names)
        assertTrue("ETag" in names)
        assertTrue("Content-Encoding" in names)
        assertTrue("Cache-Control" in names)
        assertFalse("Content-Type" in names)
        assertEquals("bytes 100-199/1000", forwarded.first { it.first == "Content-Range" }.second)
    }
}
