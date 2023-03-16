package com.sf.animescraper.ui.utils

import com.sf.animescraper.App
import com.sf.animescraper.R
import com.sf.animescraper.network.requests.okhttp.HttpError
import io.reactivex.rxjava3.core.Observable
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