package com.sf.tadami.ui.utils

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.sf.tadami.App

object UiToasts {
    fun showToast(msg : String){
        val context = App.getAppContext()
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(
                context,
                msg,
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Handler(Looper.getMainLooper()).post{
                Toast.makeText(
                    context,
                    msg,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }
    fun showToast(stringRes : Int,vararg args : String) {
        val context = App.getAppContext() ?: return;
        showToast(context.getString(stringRes,*args))
    }
}