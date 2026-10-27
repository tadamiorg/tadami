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
