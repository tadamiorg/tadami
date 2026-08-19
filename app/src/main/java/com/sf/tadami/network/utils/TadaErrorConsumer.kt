package com.sf.tadami.network.utils

import android.app.Application
import com.sf.tadami.network.HttpException
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.utils.formattedMessage
import com.sf.tadami.ui.utils.isKnownError
import com.sf.tadami.utils.logcat
import io.reactivex.rxjava3.functions.Consumer
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get


class TadaErrorConsumer(
    private val showUnknownError: Boolean = true,
    private val callback: ((error : Throwable,message: String?, errorCode: Int?) -> Unit)? = null
) : Consumer<Throwable> {
    override fun accept(e: Throwable) {
        logcat(LogPriority.ERROR, e) { "Request failed" }

        // The message mapping lives in ThrowableExtensions; this class used to carry its own copy,
        // including an `else` branch that reported the consumer's class name instead of the error's.
        val isKnown = e.isKnownError()
        if (showUnknownError || isKnown) {
            UiToasts.showToast(msg = with(Injekt.get<Application>()) { e.formattedMessage })
        }

        // UiToasts already hops to the main thread, so this no longer needs to block on one.
        callback?.invoke(e, e.message, (e as? HttpException)?.code)
    }
}
