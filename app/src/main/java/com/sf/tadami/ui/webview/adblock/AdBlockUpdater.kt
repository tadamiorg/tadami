package com.sf.tadami.ui.webview.adblock

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

/** Per list: the validators of the last download, for conditional requests, and its rule count. */
@Serializable
data class FilterListState(
    val etag: String? = null,
    val lastModified: String? = null,
    val fetchedAt: Long = 0,
    val ruleCount: Int = 0,
)

/**
 * @property compiledLists the lists in the compiled rules, null while the bundled list is used
 * @property lastSuccess when every list was last fetched (or found unchanged) and compiled
 */
@Serializable
data class AdBlockState(
    val lists: Map<String, FilterListState> = emptyMap(),
    val compiledLists: List<String>? = null,
    val lastSuccess: Long = 0,
    val lastAttempt: Long = 0,
    val ruleCount: Int = 0,
    val blockedHosts: Int = 0,
    val exceptionHosts: Int = 0,
    val compiledSize: Long = 0,
)

/**
 * Downloads the filter lists into [dir] (with conditional requests: an unchanged list isn't
 * downloaded again) and compiles them into `rules.bin`, read at startup instead of parsing the lists.
 * The previous rules stay when a download or the compilation fails, and files are replaced
 * atomically.
 */
class AdBlockUpdater(
    private val dir: File,
    private val client: OkHttpClient,
    private val clock: () -> Long = System::currentTimeMillis,
    private val siteOf: (String) -> String = AdBlockRules::registrableDomain,
    private val urlOf: (FilterList) -> String = FilterList::url,
) {
    private val rulesFile get() = File(dir, "rules.bin")
    private val stateFile get() = File(dir, "state.json")
    private fun listFile(list: FilterList) = File(dir, "lists/${list.id}.txt")

    /**
     * @property rules the new rules, null when they didn't change
     * @property failed the lists that couldn't be downloaded (or had no rules), kept as they were
     */
    class Result(val rules: AdBlockRules?, val state: AdBlockState, val failed: List<FilterList>)

    fun readState(): AdBlockState {
        return try {
            if (stateFile.exists()) json.decodeFromString(AdBlockState.serializer(), stateFile.readText()) else AdBlockState()
        } catch (e: Exception) {
            AdBlockState()
        }
    }

    /** The compiled rules, or null when there are none yet or they can't be read. */
    fun readRules(): AdBlockRules? {
        if (!rulesFile.exists()) return null
        return try {
            DataInputStream(BufferedInputStream(rulesFile.inputStream(), BUFFER_SIZE)).use { AdBlockRules.read(it, siteOf) }
        } catch (e: IOException) {
            null
        }
    }

    /** Fetches the [lists] that changed and compiles them. Blocking. */
    fun update(lists: List<FilterList>): Result = synchronized(LOCK) {
        File(dir, "lists").mkdirs()
        val previous = readState()
        val now = clock()
        val listStates = previous.lists.toMutableMap()
        val failed = ArrayList<FilterList>()
        val downloads = LinkedHashMap<FilterList, Download>()

        for (list in lists) {
            try {
                fetch(list, listStates[list.id]?.takeIf { listFile(list).exists() })?.let { downloads[list] = it }
            } catch (e: IOException) {
                failed += list
            }
        }

        val ids = lists.map { it.id }
        val unchanged = downloads.isEmpty() && previous.compiledLists == ids && rulesFile.exists()
        val missing = lists.any { it !in downloads && !listFile(it).exists() }
        if (unchanged || missing) {
            // Nothing new, or not every list to compile: the rules stay. Fresh lists are kept for next time
            downloads.forEach { (list, download) ->
                val count = download.file.bufferedReader().useLines { FilterParser().addAll(it) }
                if (count > 0) {
                    move(download.file, listFile(list))
                    listStates[list.id] = download.state.copy(ruleCount = count)
                } else {
                    download.file.delete()
                    failed += list
                }
            }
            val state = previous.copy(
                lists = listStates,
                lastAttempt = now,
                lastSuccess = if (failed.isEmpty() && unchanged) now else previous.lastSuccess,
            )
            writeState(state)
            return Result(null, state, failed)
        }

        val parser = FilterParser()
        for (list in lists) {
            val download = downloads[list]
            if (download != null) {
                val count = download.file.bufferedReader().useLines { parser.addAll(it) }
                if (count > 0) {
                    move(download.file, listFile(list))
                    listStates[list.id] = download.state.copy(ruleCount = count)
                    continue
                }
                // Not a filter list (an error page served as a success): the previous one stays
                download.file.delete()
                failed += list
            }
            if (listFile(list).exists()) {
                val count = listFile(list).bufferedReader().useLines { parser.addAll(it) }
                listStates[list.id] = (listStates[list.id] ?: FilterListState()).copy(ruleCount = count)
            }
        }
        val rules = parser.build(siteOf)

        val tmp = File(dir, "rules.bin.tmp")
        DataOutputStream(BufferedOutputStream(tmp.outputStream(), BUFFER_SIZE)).use { rules.write(it) }
        move(tmp, rulesFile)

        val state = AdBlockState(
            lists = listStates,
            compiledLists = ids,
            lastSuccess = if (failed.isEmpty()) now else previous.lastSuccess,
            lastAttempt = now,
            ruleCount = lists.sumOf { listStates[it.id]?.ruleCount ?: 0 },
            blockedHosts = rules.blocks.size,
            exceptionHosts = rules.exceptions.size,
            compiledSize = rulesFile.length(),
        )
        writeState(state)
        return Result(rules, state, failed)
    }

    private class Download(val file: File, val state: FilterListState)

    /** Null when the list didn't change since [known]. */
    private fun fetch(list: FilterList, known: FilterListState?): Download? {
        val request = Request.Builder().url(urlOf(list)).apply {
            known?.etag?.let { header("If-None-Match", it) }
            known?.lastModified?.let { header("If-Modified-Since", it) }
        }.build()
        client.newCall(request).execute().use { response ->
            if (response.code == HTTP_NOT_MODIFIED && known != null) return null
            if (!response.isSuccessful) throw IOException("${list.id}: HTTP ${response.code}")
            val file = File(dir, "lists/${list.id}.txt.tmp")
            file.sink().buffer().use { it.writeAll(response.body.source()) }
            return Download(
                file,
                FilterListState(
                    etag = response.header("ETag"),
                    lastModified = response.header("Last-Modified"),
                    fetchedAt = clock(),
                ),
            )
        }
    }

    private fun writeState(state: AdBlockState) {
        val tmp = File(dir, "state.json.tmp")
        tmp.writeText(json.encodeToString(AdBlockState.serializer(), state))
        move(tmp, stateFile)
    }

    private fun move(from: File, to: File) {
        Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    companion object {
        /** The lists are updated every few days, like their own `Expires` header asks. */
        const val UPDATE_INTERVAL_DAYS = 4L

        /** Opening the WebView updates lists older than this, in case the periodic update couldn't run. */
        val STALE_AFTER_MILLIS = TimeUnit.DAYS.toMillis(7)

        /** Opening the WebView doesn't retry sooner after an attempt. */
        val RETRY_AFTER_MILLIS = TimeUnit.HOURS.toMillis(1)

        private const val HTTP_NOT_MODIFIED = 304
        private const val BUFFER_SIZE = 64 * 1024
        private val LOCK = Any()
        private val json = Json { ignoreUnknownKeys = true }

        /** Whether opening the WebView should update the lists: never compiled, other lists, or stale. */
        fun needsUpdate(state: AdBlockState, lists: List<FilterList>, now: Long): Boolean {
            if (state.lastAttempt in (now - RETRY_AFTER_MILLIS + 1)..now) return false
            if (state.compiledLists != lists.map { it.id }) return true
            // A clock set back counts as stale
            return now - state.lastSuccess !in 0 until STALE_AFTER_MILLIS
        }
    }
}
