package com.sf.tadami.ui.utils

import com.sf.tadami.shared.resources.*
import com.sf.tadami.utils.stringRes
import com.sf.tadami.data.anime.NoResultException
import com.sf.tadami.extension.util.ExtensionCodeException
import com.sf.tadami.extension.util.ExtensionDisabledException
import com.sf.tadami.network.CloudflareChallengeException
import com.sf.tadami.network.HttpException
import com.sf.tadami.source.StubSource.SourceNotInstalledException
import com.sf.tadami.utils.isOnline
import java.net.UnknownHostException

/** Whether [formattedMessage] is worth showing even when unknown errors are not shown. */
fun Throwable.isKnownError(): Boolean = when (this) {
    is HttpException,
    is UnknownHostException,
    is NoResultException,
    is SourceNotInstalledException,
    is ExtensionDisabledException -> true

    else -> false
}

val Throwable.formattedMessage: String
    get() {
        when (this) {
            is HttpException -> return stringRes(Res.string.request_error_response, "$code")
            is UnknownHostException -> {
                return if (!isOnline()) {
                    stringRes(Res.string.exception_offline)
                } else {
                    stringRes(Res.string.exception_unknown_host, message ?: "")
                }
            }

            is CloudflareChallengeException -> return message ?: stringRes(Res.string.request_bypass_cloudflare_failure)
            is NoResultException -> return stringRes(Res.string.pager_no_results)
            is SourceNotInstalledException -> return stringRes(Res.string.source_not_installed, sourceName)
            is ExtensionDisabledException -> return stringRes(Res.string.ext_crashed_disabled, sourceName)
            // The error of the extension itself
            is ExtensionCodeException -> return message.orEmpty()
        }
        return when (val className = this::class.simpleName) {
            "Exception", "IOException" -> message ?: className
            else -> "$className: $message"
        }
    }