package com.sf.tadami.ui.utils

import android.util.Log
import com.sf.tadami.R
import com.sf.tadami.network.HttpError
import com.sf.tadami.source.online.StubSource
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.rx3.await

suspend fun <T : Any> Observable<T>.awaitSingleOrNull(
    printErrors: Boolean = true,
    onError: (() -> Unit)? = null
): T? {
    return try {
        this.singleOrError().await()
    } catch (e: Exception) {
        onError?.invoke()
        if (printErrors) {
            when (e) {

                is HttpError.Failure -> {
                    UiToasts.showToast(
                        stringRes = R.string.request_error_response,
                        args = arrayOf("${e.statusCode}")
                    )
                }

                is HttpError.CloudflareError -> {
                    UiToasts.showToast(R.string.request_bypass_cloudflare_failure)
                }

                is CancellationException -> {
                    Log.e("AwaitSingleOrNull", "Cancellation : ${e.message}")
                }

                is StubSource.SourceNotInstalledException -> {
                    UiToasts.showToast(
                        stringRes = R.string.source_not_installed,
                        args = arrayOf("${e.message}")
                    )
                }

                else -> {
                    UiToasts.showToast(
                        stringRes = R.string.request_unknown_error,
                        args = arrayOf("${e.message}")
                    )
                    e.printStackTrace()
                }
            }
        }
        null
    }
}

suspend fun <T : Any> Observable<T>.awaitSingleOrError(): T {
    return this.singleOrError().await()
}
