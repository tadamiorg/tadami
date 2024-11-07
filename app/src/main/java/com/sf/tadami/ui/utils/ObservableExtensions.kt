package com.sf.tadami.ui.utils

import android.util.Log
import com.sf.tadami.App
import com.sf.tadami.R
import com.sf.tadami.data.anime.NoResultException
import com.sf.tadami.network.HttpException
import com.sf.tadami.source.StubSource.SourceNotInstalledException
import com.sf.tadami.utils.isOnline
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.rx3.await
import java.net.UnknownHostException

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

                is HttpException -> {
                    App.getAppContext()?.let {
                        UiToasts.showToast(
                            stringRes = R.string.request_error_response,
                            args = arrayOf("${e.code}")
                        )
                    }
                }

                is UnknownHostException -> {
                    App.getAppContext()?.let {
                        val actualError = if (!it.isOnline()) {
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
                }

                is NoResultException -> {
                    App.getAppContext()?.let {
                        UiToasts.showToast(
                            msg = it.getString(R.string.pager_no_results)
                        )
                    }
                }

                is CancellationException -> {
                    Log.e("AwaitSingleOrNull", "Cancellation : ${e.message}")
                }

                else -> {

                    App.getAppContext()?.let {
                        val unknownError = when (val className = this::class.simpleName) {
                            "Exception", "IOException" -> e.message ?: className
                            else -> "$className: ${e.message}"
                        }
                        UiToasts.showToast(
                            msg = unknownError
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
