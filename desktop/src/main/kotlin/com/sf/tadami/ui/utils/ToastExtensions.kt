package com.sf.tadami.ui.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** The extensions' toasts, shown by the desktop window as snackbars. */
object UiToasts {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun showToast(msg: String, duration: Int = 0) {
        _messages.tryEmit(msg)
    }

    fun showToast(stringRes: Int, duration: Int = 0, vararg args: Any) {
        _messages.tryEmit("Message $stringRes")
    }
}
