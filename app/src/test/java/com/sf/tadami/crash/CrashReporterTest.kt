package com.sf.tadami.crash

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReporterTest {

    private val day = 24L * 60 * 60 * 1000

    private val log = CrashReporter.CrashLog(
        stacktrace = "java.lang.IllegalStateException",
        versionName = "1.10.0",
        versionCode = 42,
        buildDate = "2026-10-14",
        androidVersion = "15",
        device = "Device",
        webView = "WebView",
        extensions = "",
        timestamp = 1_000L * day,
    )

    @Test
    fun `keeps a recent report of the same version`() {
        assertFalse(CrashReporter.isExpired(log, now = log.timestamp + 7 * day, versionCode = 42))
    }

    @Test
    fun `expires a report older than 7 days`() {
        assertTrue(CrashReporter.isExpired(log, now = log.timestamp + 7 * day + 1, versionCode = 42))
    }

    @Test
    fun `expires a report of another version`() {
        assertTrue(CrashReporter.isExpired(log, now = log.timestamp, versionCode = 43))
    }

    @Test
    fun `formats the recent logs after the stack trace`() {
        val report = log.copy(logs = "W/Tag: line").format()

        assertTrue(report.indexOf("java.lang.IllegalStateException") < report.indexOf("--- Recent logs ---"))
        assertTrue(report.contains("W/Tag: line"))
    }
}
