package com.sf.tadami.crash

import logcat.LogPriority
import logcat.LogcatLogger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Keeps the most recent log lines in memory so a crash report can carry the context that led up to
 * the crash.
 *
 * Reading them back from logcat isn't good enough: `Log.d` is stripped from release builds by
 * ProGuard, and the system ring buffer is shared with every other app, so by the time the user
 * taps "share" the interesting lines are usually gone.
 */
object CrashLogBuffer : LogcatLogger {

    private const val MAX_ENTRIES = 500

    private val timestampFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

    private val entries = ArrayDeque<String>(MAX_ENTRIES)

    override fun isLoggable(priority: LogPriority, tag: String) = true

    override fun log(priority: LogPriority, tag: String, message: String) {
        val line = "${timestampFormat.format(Date())} ${priority.name.first()}/$tag: $message"
        synchronized(entries) {
            if (entries.size >= MAX_ENTRIES) entries.removeFirst()
            entries.addLast(line)
        }
    }

    fun snapshot(): String = synchronized(entries) { entries.joinToString("\n") }

    fun clear() = synchronized(entries) { entries.clear() }
}
