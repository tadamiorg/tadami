package com.sf.tadami.network.requests.utils

import com.sf.tadami.network.requests.okhttp.Callback
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.App
import com.sf.tadami.R
import com.sf.tadami.network.requests.okhttp.HttpError
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class TadaObserver<T : Any>(private val callback: Callback<T>? = null,private val showUnknownError: Boolean = true) : Observer<T> {
    override fun onSubscribe(d: Disposable) {}

    override fun onNext(data: T) {
        runBlocking {
            withContext(Dispatchers.Main) {
                callback?.onData(data)
            }
        }

    }

    override fun onError(e: Throwable) {
        runBlocking {
            withContext(Dispatchers.Main) {
                when (e) {
                    is HttpError.Failure -> {
                        App.getAppContext()?.let {
                            UiToasts.showToast(
                                stringRes = R.string.request_error_response,
                                args = arrayOf("${e.statusCode}")
                            )
                        }
                        callback?.onError(e.message, e.statusCode)
                    }
                    is HttpError.CloudflareError -> {
                        App.getAppContext()
                            ?.let { UiToasts.showToast(R.string.request_bypass_cloudflare_failure) }
                        callback?.onError(e.msg)
                    }
                    else -> {
                        if(showUnknownError){
                            App.getAppContext()?.let {
                                UiToasts.showToast(
                                    stringRes = R.string.request_unknown_error,
                                    args = arrayOf("${e.message}")
                                )
                            }
                        }
                        callback?.onError(e.message)
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onComplete() {}
}


