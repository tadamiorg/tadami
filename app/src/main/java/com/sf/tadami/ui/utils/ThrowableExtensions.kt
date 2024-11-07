package com.sf.tadami.ui.utils

import android.content.Context
import com.sf.tadami.R
import com.sf.tadami.data.anime.NoResultException
import com.sf.tadami.network.HttpException
import com.sf.tadami.source.StubSource.SourceNotInstalledException
import com.sf.tadami.utils.isOnline
import java.net.UnknownHostException

context(Context)
val Throwable.formattedMessage: String
    get() {
        when (this) {
            is HttpException -> return getString(R.string.request_error_response, "$code")
            is UnknownHostException -> {
                return if (!isOnline()) {
                    getString(R.string.exception_offline)
                } else {
                    getString(R.string.exception_unknown_host, message ?: "")
                }
            }

            is NoResultException -> return getString(R.string.pager_no_results)
            is SourceNotInstalledException -> return getString(R.string.source_not_installed)
        }
        return when (val className = this::class.simpleName) {
            "Exception", "IOException" -> message ?: className
            else -> "$className: $message"
        }
    }