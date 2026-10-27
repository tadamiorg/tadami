package com.sf.tadami.utils

internal actual fun platformLog(priority: Int, tag: String?, msg: String, tr: Throwable?): Int {
    val line = "${"VDIWEA"[(priority - 2).coerceIn(0, 5)]}/$tag: $msg" + (tr?.let { '\n' + it.stackTraceToString() } ?: "")
    (if (priority >= Log.WARN) System.err else System.out).println(line)
    return line.length
}
