package com.sf.tadami.utils

internal actual fun platformLog(priority: Int, tag: String?, msg: String, tr: Throwable?): Int =
    android.util.Log.println(priority, tag, if (tr == null) msg else msg + '\n' + android.util.Log.getStackTraceString(tr))
