package com.sf.tadami.crash

import logcat.LogPriority
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashLogBufferTest {

    @After
    fun tearDown() = CrashLogBuffer.clear()

    @Test
    fun `strips the query string of urls`() {
        assertEquals(
            "GET https://example.com/search failed, then https://cdn.example.com/a.m3u8#t",
            CrashLogBuffer.stripUrlQueries(
                "GET https://example.com/search?q=secret&token=abc failed, " +
                    "then https://cdn.example.com/a.m3u8?sig=1#t",
            ),
        )
    }

    @Test
    fun `leaves a message without url query as is`() {
        val message = "Loaded https://example.com/anime/1 in 2s? yes"
        assertEquals(message, CrashLogBuffer.stripUrlQueries(message))
    }

    @Test
    fun `keeps only the last 500 lines`() {
        repeat(510) { CrashLogBuffer.log(LogPriority.WARN, "Tag", "line $it") }

        val lines = CrashLogBuffer.snapshot().lines()
        assertEquals(500, lines.size)
        assertTrue(lines.first().endsWith("W/Tag: line 10"))
        assertTrue(lines.last().endsWith("W/Tag: line 509"))
    }

    @Test
    fun `strips the queries of the stored lines`() {
        CrashLogBuffer.log(LogPriority.ERROR, "Net", "https://example.com/v?id=1")

        assertTrue(CrashLogBuffer.snapshot().endsWith("E/Net: https://example.com/v"))
    }
}
