package com.sf.tadami.ui.utils

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object UiToasts {
    private var currentToast: Toast? = null

    fun showToast(msg : String,duration : Int = Toast.LENGTH_SHORT){
        val context = Injekt.get<Application>()
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
        val context = Injekt.get<Application>()
        showToast(context.getString(stringRes,*args),duration)
    }
}