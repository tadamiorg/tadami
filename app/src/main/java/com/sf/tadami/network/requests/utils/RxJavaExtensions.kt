package com.sf.tadami.network.requests.utils

import com.sf.tadami.network.requests.okhttp.Callback
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.App
import com.sf.tadami.R
import com.sf.tadami.network.api.online.StubSource
import com.sf.tadami.network.requests.okhttp.HttpError
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class TadaErrorConsumer(
    private val showUnknownError: Boolean = true,
    private val callback: ((error : Throwable,message: String?, errorCode: Int?) -> Unit)? = null
) : Consumer<Throwable> {
    override fun accept(e: Throwable) {
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
                        callback?.invoke(e,e.message, e.statusCode)
                    }

                    is HttpError.CloudflareError -> {
                        App.getAppContext()
                            ?.let { UiToasts.showToast(R.string.request_bypass_cloudflare_failure) }
                        callback?.invoke(e,e.msg, null)
                    }

                    is StubSource.SourceNotInstalledException -> {
                        App.getAppContext()?.let {
                            UiToasts.showToast(
                                stringRes = R.string.source_not_installed,
                                args = arrayOf("${e.message}")
                            )
                        }
                        callback?.invoke(e,e.message, null)
                    }

                    else -> {
                        if (showUnknownError) {
                            App.getAppContext()?.let {
                                UiToasts.showToast(
                                    stringRes = R.string.request_unknown_error,
                                    args = arrayOf("${e.message}")
                                )
                            }
                        }
                        callback?.invoke(e,e.message, null)
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}


