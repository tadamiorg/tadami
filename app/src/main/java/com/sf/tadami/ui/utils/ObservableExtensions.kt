package com.sf.tadami.ui.utils

import android.util.Log
import com.sf.tadami.App
import com.sf.tadami.R
import com.sf.tadami.network.requests.okhttp.HttpError
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.rx3.await

suspend fun <T : Any>Observable<T>.awaitSingleOrError(
    onError: (() -> Unit)? = null
): T? {
    return try {
        this.singleOrError().await()
    }
    catch(e : Exception) {
        onError?.invoke()
        when(e){
            is HttpError.Failure -> {
                App.getAppContext()?.let { UiToasts.showToast(
                    R.string.request_error_response,
                    "${e.statusCode}"
                )}
            }
            is HttpError.CloudflareError ->{
                App.getAppContext()?.let { UiToasts.showToast(R.string.request_bypass_cloudflare_failure) }
            }
            is CancellationException -> {
                Log.e("AwaitSingleOrError","Cancellation : ${e.message}")
            }
            else ->{
                App.getAppContext()?.let{ UiToasts.showToast(
                    R.string.request_unknown_error,
                    "${e.message}"
                )}
                e.printStackTrace()
            }
        }
        null
    }
}