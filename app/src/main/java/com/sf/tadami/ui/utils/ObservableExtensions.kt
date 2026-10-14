package com.sf.tadami.ui.utils

import android.app.Application
import android.util.Log
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
        if (printErrors) {
            if (e is CancellationException) {
                Log.d("AwaitSingleOrNull", "Cancellation : ${e.message}")
            } else {
                UiToasts.showToast(msg = with(Injekt.get<Application>()) { e.formattedMessage })
                if (!e.isKnownError()) logcat(LogPriority.ERROR, e)
            }
        }
        null
    }
}

suspend fun <T : Any> Observable<T>.awaitSingleOrError(): T {
    return this.singleOrError().await()
}
