package com.sf.tadami.ui.utils

import android.widget.Toast

@Deprecated("Part of the app's UI, kept for the extensions using it: it will be removed from extensions-lib")
object UiToasts {
    private var currentToast: Toast? = null

    fun showToast(msg : String,duration : Int = Toast.LENGTH_SHORT){
       throw Exception("Stub")

    }
    fun showToast(stringRes : Int,duration : Int = Toast.LENGTH_SHORT,vararg args : Any) {
        throw Exception("Stub")
    }
}