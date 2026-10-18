package com.sf.tadami.ui.webview.adblock

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.TimeUnit

class AdBlockUpdaterTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var server: MockWebServer
    private var now = 1_000_000L

    private val lists = listOf(FilterList.EASYLIST, FilterList.PETER_LOWE)

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun updater(dir: File = folder.root) = AdBlockUpdater(
        dir = dir,
        client = OkHttpClient(),
        clock = { now },
        siteOf = { host -> host.split('.').takeLast(2).joinToString(".") },
        urlOf = { list -> server.url("/${list.id}.txt").toString() },
    )

    private fun list(vararg lines: String, etag: String? = null) =
        MockResponse.Builder().body(lines.joinToString("\n")).apply { etag?.let { addHeader("ETag", it) } }.build()

    private fun AdBlockRules.blocks(url: String) =
        isBlocked(AdBlockRequest.of(url, RequestKind.SUBRESOURCE, "page.com")!!)

    private fun enqueueBoth() {
        server.enqueue(list("! EasyList", "||ads.example.com^", "example.com##.ad", etag = "\"e1\""))
        server.enqueue(list("||tracker.example.net^", "||pop.example.org^\$popup", etag = "\"p1\""))
    }

    @Test
    fun downloadsAndCompilesTheLists() {
        enqueueBoth()

        val result = updater().update(lists)

        val rules = result.rules!!
        assertTrue(rules.blocks("https://ads.example.com/"))
        assertTrue(rules.blocks("https://tracker.example.net/"))
        assertTrue(result.failed.isEmpty())
        val state = result.state
        assertEquals(listOf("easylist", "peter_lowe"), state.compiledLists)
        assertEquals(now, state.lastSuccess)
        assertEquals(1, state.lists.getValue("easylist").ruleCount)
        assertEquals(2, state.lists.getValue("peter_lowe").ruleCount)
        assertEquals(3, state.ruleCount)
        assertEquals(3, state.blockedHosts)
        assertEquals("\"e1\"", state.lists.getValue("easylist").etag)
        assertTrue(File(folder.root, "rules.bin").length() > 0)
    }

    @Test
    fun theCompiledRulesAndStateAreReadBack() {
        enqueueBoth()
        val written = updater().update(lists).state

        val reader = updater()
        assertEquals(written, reader.readState())
        val rules = reader.readRules()!!
        assertTrue(rules.blocks("https://ads.example.com/"))
        assertFalse(rules.blocks("https://example.com/"))
    }

    @Test
    fun unchangedListsAreAskedWithTheirValidatorsAndNotCompiledAgain() {
        enqueueBoth()
        updater().update(lists)
        server.takeRequest()
        server.takeRequest()

        now += 1000
        server.enqueue(MockResponse.Builder().code(304).build())
        server.enqueue(MockResponse.Builder().code(304).build())
        val result = updater().update(lists)

        assertEquals("\"e1\"", server.takeRequest().headers["If-None-Match"])
        assertEquals("\"p1\"", server.takeRequest().headers["If-None-Match"])
        assertNull(result.rules)
        assertTrue(result.failed.isEmpty())
        assertEquals(now, result.state.lastSuccess)
        assertTrue(updater().readRules()!!.blocks("https://ads.example.com/"))
    }

    @Test
    fun aFailedDownloadKeepsThePreviousListAndRules() {
        enqueueBoth()
        updater().update(lists)
        val firstSuccess = now

        now += 1000
        server.enqueue(MockResponse.Builder().code(500).build())
        server.enqueue(list("||other.example.net^", etag = "\"p2\""))
        val result = updater().update(lists)

        assertEquals(listOf(FilterList.EASYLIST), result.failed)
        val rules = result.rules!!
        // EasyList as it was, Peter Lowe's list updated
        assertTrue(rules.blocks("https://ads.example.com/"))
        assertTrue(rules.blocks("https://other.example.net/"))
        assertFalse(rules.blocks("https://tracker.example.net/"))
        assertEquals(firstSuccess, result.state.lastSuccess)
        assertEquals(now, result.state.lastAttempt)
    }

    @Test
    fun aDownloadWithoutRulesIsRefused() {
        enqueueBoth()
        updater().update(lists)

        server.enqueue(list("<html><body>Captive portal</body></html>"))
        server.enqueue(MockResponse.Builder().code(304).build())
        val result = updater().update(lists)

        assertEquals(listOf(FilterList.EASYLIST), result.failed)
        assertTrue(updater().readRules()!!.blocks("https://ads.example.com/"))
        assertEquals("\"e1\"", result.state.lists.getValue("easylist").etag)
    }

    @Test
    fun withoutEveryListTheBundledRulesStay() {
        server.enqueue(MockResponse.Builder().code(503).build())
        server.enqueue(list("||tracker.example.net^"))

        val result = updater().update(lists)

        assertNull(result.rules)
        assertNull(updater().readRules())
        assertNull(result.state.compiledLists)
        assertEquals(0L, result.state.lastSuccess)
        // Kept for the next attempt
        assertTrue(File(folder.root, "lists/peter_lowe.txt").exists())
    }

    @Test
    fun turningAListOffCompilesTheOthersWithoutDownloadingThemAgain() {
        enqueueBoth()
        updater().update(lists)

        server.enqueue(MockResponse.Builder().code(304).build())
        val result = updater().update(listOf(FilterList.PETER_LOWE))

        val rules = result.rules!!
        assertFalse(rules.blocks("https://ads.example.com/"))
        assertTrue(rules.blocks("https://tracker.example.net/"))
        assertEquals(listOf("peter_lowe"), result.state.compiledLists)
    }

    @Test
    fun noListCompilesRulesBlockingNothing() {
        enqueueBoth()
        updater().update(lists)

        val result = updater().update(emptyList())

        assertFalse(result.rules!!.blocks("https://ads.example.com/"))
        assertEquals(emptyList<String>(), result.state.compiledLists)
    }

    @Test
    fun aCorruptedRulesFileIsIgnored() {
        File(folder.root, "rules.bin").writeText("garbage")
        File(folder.root, "state.json").writeText("{not json")

        assertNull(updater().readRules())
        assertEquals(AdBlockState(), updater().readState())
    }

    @Test
    fun theWebViewUpdatesListsNeverCompiledChangedOrStale() {
        val day = TimeUnit.DAYS.toMillis(1)
        val now = 100 * day
        val fresh = AdBlockState(compiledLists = lists.map { it.id }, lastSuccess = now - day, lastAttempt = now - day)

        assertTrue(AdBlockUpdater.needsUpdate(AdBlockState(), lists, now))
        assertFalse(AdBlockUpdater.needsUpdate(fresh, lists, now))
        assertTrue(AdBlockUpdater.needsUpdate(fresh, listOf(FilterList.EASYLIST), now))
        assertTrue(AdBlockUpdater.needsUpdate(fresh.copy(lastSuccess = now - 8 * day), lists, now))
        assertFalse(AdBlockUpdater.needsUpdate(fresh.copy(lastSuccess = now - 6 * day), lists, now))
        // A clock set back
        assertTrue(AdBlockUpdater.needsUpdate(fresh.copy(lastSuccess = now + day, lastAttempt = now - day), lists, now))
    }

    @Test
    fun theAppDownloadsListsNeverCompiledOrChangedAsItStarts() {
        val day = TimeUnit.DAYS.toMillis(1)
        val compiled = AdBlockState(compiledLists = lists.map { it.id }, lastSuccess = 1, lastAttempt = 1)

        assertTrue(AdBlockUpdater.needsDownload(AdBlockState(), lists))
        assertTrue(AdBlockUpdater.needsDownload(compiled, listOf(FilterList.EASYLIST)))
        // Stale rules wait for the periodic update, or for the WebView to open
        assertFalse(AdBlockUpdater.needsDownload(compiled.copy(lastSuccess = 1 - 30 * day), lists))
        assertFalse(AdBlockUpdater.needsDownload(compiled, lists))
    }

    @Test
    fun theWebViewWaitsBeforeRetryingAFailedUpdate() {
        val hour = TimeUnit.HOURS.toMillis(1)
        val now = 1000 * hour
        val failed = AdBlockState(lastAttempt = now - hour / 2)

        assertFalse(AdBlockUpdater.needsUpdate(failed, lists, now))
        assertTrue(AdBlockUpdater.needsUpdate(failed.copy(lastAttempt = now - 2 * hour), lists, now))
    }
}
