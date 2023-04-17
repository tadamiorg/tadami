package com.sf.tadami.ui.utils

import android.util.Log
import com.sf.tadami.App
import com.sf.tadami.R
import com.sf.tadami.network.requests.okhttp.HttpError
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.rx3.await

suspend fun <T : Any> Observable<T>.awaitSingleOrNull(
    printErrors : Boolean = true,
    onError: (() -> Unit)? = null
): T? {
    return try {
        this.singleOrError().await()
    } catch (e: Exception) {
        onError?.invoke()
        if(printErrors){
            when (e) {

                is HttpError.Failure -> {
                    App.getAppContext()?.let {
                        UiToasts.showToast(
                            stringRes = R.string.request_error_response,
                            args = arrayOf("${e.statusCode}")
                        )
                    }
                }
                is HttpError.CloudflareError -> {
                    App.getAppContext()
                        ?.let { UiToasts.showToast(R.string.request_bypass_cloudflare_failure) }
                }
                is CancellationException -> {
                    Log.e("AwaitSingleOrNull", "Cancellation : ${e.message}")
                }
                else -> {
                    App.getAppContext()?.let {
                        UiToasts.showToast(
                            stringRes = R.string.request_unknown_error,
                            args = arrayOf("${e.message}")
                        )
                    }
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
