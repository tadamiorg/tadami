package com.sf.tadami.network.utils

import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import com.sf.tadami.network.HttpException
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.utils.formattedMessage
import com.sf.tadami.ui.utils.isKnownError
import com.sf.tadami.utils.logcat
import io.reactivex.rxjava3.functions.Consumer
import logcat.LogPriority


class TadaErrorConsumer(
    private val showUnknownError: Boolean = true,
    private val callback: ((error : Throwable,message: String?, errorCode: Int?) -> Unit)? = null
) : Consumer<Throwable> {
    override fun accept(e: Throwable) {
        val isKnownError = e.isKnownError()
        // UiToasts already posts to the main thread
        if (showUnknownError || isKnownError) {
            UiToasts.showToast(msg = e.formattedMessage)
        }
        if (!isKnownError) logcat(LogPriority.ERROR, e)

        val callback = callback ?: return
        val errorCode = (e as? HttpException)?.code
        // The callback still runs on the main thread, without blocking the calling one: at once when on it
        CoroutineScope(Dispatchers.Main.immediate).launch { callback(e, e.message, errorCode) }
    }
}


