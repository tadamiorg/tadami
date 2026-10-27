package com.sf.tadami.utils

import android.app.Application
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.sf.tadami.ui.utils.UiToasts
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

actual fun isOnline(): Boolean {
    val connectivityManager = Injekt.get<Application>().getSystemService<ConnectivityManager>() ?: return false
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
    val maxTransport = NetworkCapabilities.TRANSPORT_LOWPAN
    return (NetworkCapabilities.TRANSPORT_CELLULAR..maxTransport).any(networkCapabilities::hasTransport)
}

actual fun openInBrowser(url: String) {
    try {
        Injekt.get<Application>().startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (e: Exception) {
        UiToasts.showToast(e.message ?: "")
    }
}
