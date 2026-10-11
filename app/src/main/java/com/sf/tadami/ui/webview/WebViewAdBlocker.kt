package com.sf.tadami.ui.webview

import android.content.Context
import android.util.Log
import com.sf.tadami.R

/** App-wide ad host list for the WebView, parsed once off the main thread. */
object WebViewAdBlocker {

    @Volatile
    private var blocklist: HostBlocklist? = null

    fun isBlocked(host: String?): Boolean {
        return blocklist?.isBlocked(host) == true
    }

    /** Blocking, call it from a background thread. */
    @Synchronized
    fun load(context: Context) {
        if (blocklist != null) return
        blocklist = try {
            context.resources.openRawResource(R.raw.adservers).use { HostBlocklist.parse(it) }
        } catch (e: Exception) {
            Log.e("WebViewAdBlocker", "Could not read the ad servers list", e)
            HostBlocklist.EMPTY
        }
    }
}
