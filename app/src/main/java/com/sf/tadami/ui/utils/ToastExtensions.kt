package com.sf.tadami.ui.utils

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.sf.tadami.App

object UiToasts {
    private var currentToast: Toast? = null

    fun showToast(msg : String,duration : Int = Toast.LENGTH_SHORT){
        val context = App.getAppContext()
        currentToast?.cancel()
        if (Looper.myLooper() == Looper.getMainLooper()) {
            currentToast = Toast.makeText(
                context,
                msg,
                duration
            )
            currentToast?.show()
        } else {
            Handler(Looper.getMainLooper()).post{
                currentToast = Toast.makeText(
                    context,
                    msg,
                    duration
                )
                currentToast?.show()
            }
        }

    }
    fun showToast(stringRes : Int,duration : Int = Toast.LENGTH_SHORT,vararg args : Any) {
        val context = App.getAppContext() ?: return
        showToast(context.getString(stringRes,*args),duration)
    }
}