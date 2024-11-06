package com.sf.tadami.ui.utils

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

fun Long.formatMinSec(): String {
    return if (this == 0L) {
        "00:00"
    } else {
        val hours = TimeUnit.MILLISECONDS.toHours(this)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(this) - TimeUnit.HOURS.toMinutes(hours)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(this) - TimeUnit.MINUTES.toSeconds(minutes) - TimeUnit.HOURS.toSeconds(hours)
        when {
            hours > 0 -> String.format(Locale.getDefault(),"%02d:%02d:%02d", hours, minutes, seconds)
            else -> String.format(Locale.getDefault(),"%02d:%02d", minutes, seconds)
        }
    }
}
fun Long.formatDate(): String {
    val dateFormat = SimpleDateFormat("dd/MM - HH:mm", Locale.getDefault())
    return dateFormat.format(this)
}


