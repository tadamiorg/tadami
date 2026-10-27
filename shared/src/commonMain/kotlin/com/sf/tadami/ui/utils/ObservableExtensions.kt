package com.sf.tadami.ui.utils

import com.sf.tadami.utils.Log
import com.sf.tadami.utils.logcat
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.rx3.await
import logcat.LogPriority

suspend fun <T : Any> Observable<T>.awaitSingleOrNull(
    printErrors: Boolean = true,
    onError: (() -> Unit)? = null
): T? = awaitOrNull(printErrors, onError) { singleOrError().await() }

/**
 * What [block] returns, or null when it fails: then [onError] runs, and the error is shown in a toast
 * when [printErrors].
 */
suspend fun <T : Any> awaitOrNull(
    printErrors: Boolean = true,
    onError: (() -> Unit)? = null,
    block: suspend () -> T,
): T? {
    return try {
        block()
    } catch (e: Exception) {
        onError?.invoke()
        if (printErrors) {
            if (e is CancellationException) {
                Log.d("AwaitSingleOrNull", "Cancellation : ${e.message}")
            } else {
                UiToasts.showToast(msg = e.formattedMessage)
                if (!e.isKnownError()) e.logcat(LogPriority.ERROR, e, tag = "AwaitSingleOrNull")
            }
        }
        null
    }
}

suspend fun <T : Any> Observable<T>.awaitSingleOrError(): T {
    return this.singleOrError().await()
}
