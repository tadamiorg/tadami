package com.sf.tadami.ui.utils

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.sf.tadami.App

object UiToasts {
    fun showToast(msg : String,duration : Int = Toast.LENGTH_SHORT){
        val context = App.getAppContext()
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(
                context,
                msg,
                duration
            ).show()
        } else {
            Handler(Looper.getMainLooper()).post{
                Toast.makeText(
                    context,
                    msg,
                    duration
                ).show()
            }
        }

    }
    fun showToast(stringRes : Int,duration : Int = Toast.LENGTH_SHORT,vararg args : String) {
        val context = App.getAppContext() ?: return
        showToast(context.getString(stringRes,*args),duration)
    }
}