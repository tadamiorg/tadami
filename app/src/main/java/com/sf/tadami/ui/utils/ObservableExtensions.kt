package com.sf.tadami.ui.utils

import android.app.Application
import com.sf.tadami.utils.logcat
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.rx3.await
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

suspend fun <T : Any> Observable<T>.awaitSingleOrNull(
    printErrors: Boolean = true,
    onError: (() -> Unit)? = null
): T? {
    return try {
        this.singleOrError().await()
    } catch (e: Exception) {
        onError?.invoke()

        if (e is CancellationException) {
            logcat { "Cancelled: ${e.message}" }
            return null
        }

        logcat(LogPriority.ERROR, e) { "Observable failed" }
        if (printErrors) {
            // Single source of truth for turning a throwable into user-facing text; this used to
            // be a copy of the `when` in ThrowableExtensions, and its `else` branch read
            // `this::class.simpleName` — the Observable's name, not the exception's.
            UiToasts.showToast(msg = with(Injekt.get<Application>()) { e.formattedMessage })
        }
        null
    }
}

suspend fun <T : Any> Observable<T>.awaitSingleOrError(): T {
    return this.singleOrError().await()
}
