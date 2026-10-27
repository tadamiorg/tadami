package com.sf.tadami.utils

/** android.util.Log for the shared code: logcat on Android, the console on the desktop. */
object Log {
    const val VERBOSE = 2
    const val DEBUG = 3
    const val INFO = 4
    const val WARN = 5
    const val ERROR = 6
    const val ASSERT = 7

    fun v(tag: String?, msg: String, tr: Throwable? = null): Int = platformLog(VERBOSE, tag, msg, tr)
    fun d(tag: String?, msg: String, tr: Throwable? = null): Int = platformLog(DEBUG, tag, msg, tr)
    fun i(tag: String?, msg: String, tr: Throwable? = null): Int = platformLog(INFO, tag, msg, tr)
    fun w(tag: String?, msg: String, tr: Throwable? = null): Int = platformLog(WARN, tag, msg, tr)
    fun w(tag: String?, tr: Throwable): Int = platformLog(WARN, tag, "", tr)
    fun e(tag: String?, msg: String, tr: Throwable? = null): Int = platformLog(ERROR, tag, msg, tr)
    fun wtf(tag: String?, msg: String, tr: Throwable? = null): Int = platformLog(ASSERT, tag, msg, tr)

    fun getStackTraceString(tr: Throwable?): String = tr?.stackTraceToString().orEmpty()
}

internal expect fun platformLog(priority: Int, tag: String?, msg: String, tr: Throwable?): Int
