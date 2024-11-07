package com.sf.tadami.network.utils

import com.sf.tadami.App
import com.sf.tadami.R
import com.sf.tadami.data.anime.NoResultException
import com.sf.tadami.network.HttpException
import com.sf.tadami.source.StubSource.SourceNotInstalledException
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.utils.isOnline
import io.reactivex.rxjava3.functions.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.UnknownHostException


class TadaErrorConsumer(
    private val showUnknownError: Boolean = true,
    private val callback: ((error : Throwable,message: String?, errorCode: Int?) -> Unit)? = null
) : Consumer<Throwable> {
    override fun accept(e: Throwable) {
        runBlocking {
            withContext(Dispatchers.Main) {
                when (e) {
                    is HttpException -> {
                        App.getAppContext()?.let {
                            UiToasts.showToast(
                                stringRes = R.string.request_error_response,
                                args = arrayOf("${e.code}")
                            )
                        }
                        callback?.invoke(e,e.message, e.code)
                    }
                    is UnknownHostException -> {
                        App.getAppContext()?.let {
                            val actualError =  if (!it.isOnline()) {
                                it.getString(R.string.exception_offline)
                            } else {
                                it.getString(R.string.exception_unknown_host, e.message ?: "")
                            }
                            UiToasts.showToast(
                                msg = actualError
                            )
                        }
                    }
                    is SourceNotInstalledException -> {
                        App.getAppContext()?.let {
                            UiToasts.showToast(
                                stringRes = R.string.source_not_installed,
                                args = arrayOf("${e.message}")
                            )
                        }
                        callback?.invoke(e,e.message, null)
                    }

                    is NoResultException -> {
                        App.getAppContext()?.let {
                            UiToasts.showToast(
                                msg = it.getString(R.string.pager_no_results)
                            )
                        }
                        callback?.invoke(e,e.message, null)
                    }

                    else -> {
                        if (showUnknownError) {
                            App.getAppContext()?.let {
                                val unknownError = when (val className = this::class.simpleName) {
                                    "Exception", "IOException" -> e.message ?: className
                                    else -> "$className: ${e.message}"
                                }
                                UiToasts.showToast(
                                    msg = unknownError
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


