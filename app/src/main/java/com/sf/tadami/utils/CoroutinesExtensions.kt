package com.sf.tadami.utils

import android.util.Log
import androidx.work.CoroutineWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun CoroutineScope.launchIO(block: suspend CoroutineScope.() -> Unit) : Job = launch(Dispatchers.IO, block = block)

suspend fun CoroutineWorker.setForegroundSafely() {
    try {
        setForeground(getForegroundInfo())
        delay(500)
    } catch (e: IllegalStateException) {
        Log.e("Foreground service","Not allowed to set foreground job")
    }
}