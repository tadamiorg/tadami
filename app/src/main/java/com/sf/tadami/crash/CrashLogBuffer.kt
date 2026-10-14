package com.sf.tadami.crash

import com.sf.tadami.BuildConfig
import logcat.LogPriority
import logcat.LogcatLogger
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Keeps the last log lines in memory for the crash report: release builds strip `Log.d`, and the
 * system logcat buffer, shared with every app, has usually lost them by the time the user shares.
 */
object CrashLogBuffer : LogcatLogger {

    private const val MAX_ENTRIES = 500

    private val timestampFormat = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS")

    // The query string of a url can hold tokens or personal search terms
    private val urlQuery = Regex("""(https?://[^\s?#]*)\?[^\s#]*""")

    private val entries = ArrayDeque<String>(MAX_ENTRIES)

    override fun isLoggable(priority: LogPriority, tag: String) =
        BuildConfig.DEBUG || priority.priorityInt >= LogPriority.INFO.priorityInt

    override fun log(priority: LogPriority, tag: String, message: String) {
        val line = "${timestampFormat.format(LocalDateTime.now())} ${priority.name.first()}/$tag: " +
            stripUrlQueries(message)
        synchronized(entries) {
            if (entries.size >= MAX_ENTRIES) entries.removeFirst()
            entries.addLast(line)
        }
    }

    fun snapshot(): String = synchronized(entries) { entries.joinToString("\n") }

    fun clear() = synchronized(entries) { entries.clear() }

    internal fun stripUrlQueries(message: String) = message.replace(urlQuery, "\$1")
}
