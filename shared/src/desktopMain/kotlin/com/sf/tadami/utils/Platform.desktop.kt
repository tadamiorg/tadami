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
