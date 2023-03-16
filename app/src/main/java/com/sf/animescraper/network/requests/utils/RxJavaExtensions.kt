package com.sf.animescraper.network.requests.utils

import com.sf.animescraper.network.requests.okhttp.Callback
import com.sf.animescraper.ui.utils.UiToasts
import com.sf.animescraper.App
import com.sf.animescraper.R
import com.sf.animescraper.network.requests.okhttp.HttpError
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


open class ObserverAS<T : Any>(private val callback : Callback<T>? = null) : Observer<T>{
    override fun onSubscribe(d: Disposable) {}

    override fun onNext(data: T) {
        runBlocking {
            withContext(Dispatchers.Main){
                callback?.onData(data)
            }
        }

    }

    override fun onError(e: Throwable) {
        runBlocking {
            withContext(Dispatchers.Main){
                when(e){
                    is HttpError.Failure -> {
                        App.getAppContext()?.let { UiToasts.showToast(
                            R.string.request_error_response,
                            "${e.statusCode}"
                        )}
                        callback?.onError(e.message,e.statusCode)
                    }
                    is HttpError.CloudflareError ->{
                        App.getAppContext()?.let { UiToasts.showToast(R.string.request_bypass_cloudflare_failure) }
                        callback?.onError(e.msg)
                    }
                    else ->{
                        App.getAppContext()?.let{ UiToasts.showToast(
                            R.string.request_unknown_error,
                            "${e.message}"
                        )}
                        callback?.onError(e.message)
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onComplete() {}
}


