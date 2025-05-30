package com.sf.tadami.utils



import logcat.LogPriority
import logcat.asLog
import logcat.logcat

inline fun Any.logcat(
    priority: LogPriority = LogPriority.DEBUG,
    throwable: Throwable? = null,
    crossinline message: () -> String = { "" },
) = logcat(priority = priority) {
    var msg = message()
    if (throwable != null) {
        if (msg.isNotBlank()) msg += "\n"
        msg += throwable.asLog()
    }
    msg
}
