package com.sf.tadami.desktop.browser

import com.sf.tadami.desktop.AppDirs
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.preferences.advanced.AdBlockPreferences
import com.sf.tadami.ui.webview.adblock.AdBlockRequest
import com.sf.tadami.ui.webview.adblock.AdBlockRules
import com.sf.tadami.ui.webview.adblock.AdBlockUpdater
import com.sf.tadami.utils.Log
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

/**
 * The ad blocker of the WebView screen, the Android app's WebViewAdBlocker: the rules compiled from the
 * filter lists, which the screen updates when they are missing or old (no background job on the
 * desktop). Until the lists were downloaded once, it blocks nothing.
 */
object DesktopAdBlocker {

    private const val TAG = "DesktopAdBlocker"

    @Volatile
    private var rules: AdBlockRules? = null

    // Lists don't need the Cloudflare handling, nor a place in the network cache
    private val updater by lazy {
        AdBlockUpdater(File(AppDirs.data, "adblock"), Injekt.get<NetworkHelper>().nonCloudflareClient.newBuilder().cache(null).build())
    }

    fun isBlocked(request: AdBlockRequest): Boolean = rules?.isBlocked(request) == true

    /** Loads the rules, then downloads the lists when they are missing or old. Blocking. */
    @Synchronized
    fun prepare(preferences: AdBlockPreferences) {
        if (rules == null) rules = updater.readRules() ?: AdBlockRules.EMPTY
        if (!AdBlockUpdater.needsUpdate(updater.readState(), preferences.lists, System.currentTimeMillis())) return
        runCatching { updater.update(preferences.lists) }
            .onSuccess { result ->
                result.rules?.let { rules = it }
                Log.i(TAG, "Filter lists: ${result.state.ruleCount} rules, failed: ${result.failed.map { it.id }}")
            }
            .onFailure { Log.e(TAG, "Could not update the filter lists", it) }
    }
}
