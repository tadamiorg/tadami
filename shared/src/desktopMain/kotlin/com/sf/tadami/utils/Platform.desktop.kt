package com.sf.tadami.utils

import com.sf.tadami.ui.utils.UiToasts
import java.awt.Desktop
import java.net.NetworkInterface
import java.net.URI

actual fun isOnline(): Boolean = runCatching {
    NetworkInterface.getNetworkInterfaces().toList().any { it.isUp && !it.isLoopback }
}.getOrDefault(true)

actual fun openInBrowser(url: String) {
    try {
        Desktop.getDesktop().browse(URI(url))
    } catch (e: Exception) {
        UiToasts.showToast(e.message ?: "")
    }
}

actual fun relativeTimeSpan(epochMillis: Long, now: Long): String {
    val elapsed = java.time.Duration.ofMillis(now - epochMillis)
    return when {
        elapsed.toHours() < 1 -> java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(epochMillis))
        elapsed.toDays() < 1 -> java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(epochMillis))
        else -> java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date(epochMillis))
    }
}

actual fun formatFileSize(bytes: Long): String = fileSize(bytes, short = false)

actual fun formatShortFileSize(bytes: Long): String = fileSize(bytes, short = true)

/** As Android's formatter: units of 1000, more decimals for the small values. */
private fun fileSize(bytes: Long, short: Boolean): String {
    val units = listOf("B", "kB", "MB", "GB", "TB", "PB")
    var value = bytes.toDouble()
    var unit = 0
    while (value > 900 && unit < units.lastIndex) {
        value /= 1000
        unit++
    }
    val decimals = when {
        unit == 0 || value >= 100 -> 0
        short && value >= 10 -> 0
        short -> 1
        else -> 2
    }
    return "%.${decimals}f %s".format(value, units[unit])
}
