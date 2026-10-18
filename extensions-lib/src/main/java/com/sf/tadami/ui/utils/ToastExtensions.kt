package com.sf.tadami.ui.utils

import android.widget.Toast

/**
 * Shows toasts, one at a time, from any thread.
 *
 * @since extensions-lib 1.5
 */
@Deprecated("Part of the app's UI, kept for the extensions using it: it will be removed from extensions-lib")
object UiToasts {
    private var currentToast: Toast? = null

    /**
     * Shows [msg], in place of the toast shown.
     *
     * @since extensions-lib 1.5
     */
    fun showToast(msg : String,duration : Int = Toast.LENGTH_SHORT){
       throw Exception("Stub")

    }

    /**
     * Shows the string [stringRes] of the app, formatted with [args], in place of the toast shown.
     *
     * @since extensions-lib 1.5
     */
    fun showToast(stringRes : Int,duration : Int = Toast.LENGTH_SHORT,vararg args : Any) {
        throw Exception("Stub")
    }
}
