package com.sf.tadami.utils



import logcat.LogPriority
import logcat.asLog
import logcat.logcat

/**
 * @param tag defaults to the simple class name of the receiver. Pass it when logging from a lambda
 * with its own receiver (a coroutine builder, `Flow.catch`...), where that name means nothing.
 */
inline fun Any.logcat(
    priority: LogPriority = LogPriority.DEBUG,
    throwable: Throwable? = null,
    tag: String? = null,
    crossinline message: () -> String = { "" },
) = logcat(priority = priority, tag = tag) {
    var msg = message()
    if (throwable != null) {
        if (msg.isNotBlank()) msg += "\n"
        msg += throwable.asLog()
    }
    msg
}
