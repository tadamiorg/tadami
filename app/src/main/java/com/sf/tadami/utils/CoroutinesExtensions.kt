package com.sf.tadami.utils

import com.sf.tadami.ui.utils.UiErrors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun CoroutineScope.launchIO(block: suspend CoroutineScope.() -> Unit) : Job = launch(Dispatchers.IO, block = block)

suspend fun <T> withNonCancellableContext(block: suspend CoroutineScope.() -> T) =
    withContext(NonCancellable, block)

/**
 * Like [launch], but a failure surfaces as a snackbar instead of an uncaught exception.
 *
 * An exception thrown inside a plain `viewModelScope.launch` reaches the default handler and
 * kills the app, which is a harsh outcome for something like a failed database read.
 *
 * @param retry offered as a Retry action on the error snackbar, when the operation is repeatable.
 */
fun CoroutineScope.launchCatching(
    context: CoroutineContext = EmptyCoroutineContext,
    retry: (() -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null,
    block: suspend CoroutineScope.() -> Unit,
): Job = launch(context) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        logcat(LogPriority.ERROR, e, tag = "launchCatching") { "Coroutine failed" }
        if (onError != null) onError(e) else UiErrors.emit(e, retry)
    }
}