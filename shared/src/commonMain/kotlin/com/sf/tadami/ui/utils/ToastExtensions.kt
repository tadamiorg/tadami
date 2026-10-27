package com.sf.tadami.ui.utils

import com.sf.tadami.utils.stringRes
import org.jetbrains.compose.resources.StringResource

/** Short messages to the user: Android toasts, snackbars on the desktop, see [display]. */
object UiToasts {
    const val LENGTH_SHORT = 0
    const val LENGTH_LONG = 1

    /** Shows a message for [duration] ([LENGTH_SHORT] or [LENGTH_LONG]), from any thread: set by each app. */
    var display: (msg: String, duration: Int) -> Unit = { msg, _ -> println("Toast: $msg") }

    /** The Android string resource [id] formatted with its arguments: set by the Android app. */
    var androidString: ((id: Int, args: Array<out Any>) -> String)? = null

    fun showToast(msg : String,duration : Int = LENGTH_SHORT){
        display(msg, duration)
    }

    /** Part of the extension API: [stringRes] is an Android string resource ID. */
    fun showToast(stringRes : Int,duration : Int = LENGTH_SHORT,vararg args : Any) {
        androidString?.invoke(stringRes, args)?.let { display(it, duration) }
    }

    fun showToast(stringRes: StringResource, duration: Int = LENGTH_SHORT, vararg args: Any) {
        display(com.sf.tadami.utils.stringRes(stringRes, *args), duration)
    }
}
