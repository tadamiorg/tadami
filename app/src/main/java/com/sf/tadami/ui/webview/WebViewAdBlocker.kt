package com.sf.tadami.ui.webview

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.ui.webview.adblock.AdBlockRequest
import com.sf.tadami.ui.webview.adblock.AdBlockRules
import com.sf.tadami.ui.webview.adblock.AdBlockState
import com.sf.tadami.ui.webview.adblock.AdBlockUpdater
import com.sf.tadami.ui.webview.adblock.FilterList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

/**
 * App-wide ad blocker of the WebView screen: the rules compiled from the filter lists. Until they were
 * downloaded once, it blocks nothing. Loaded the first time a WebView needs it.
 */
object WebViewAdBlocker {

    private const val TAG = "WebViewAdBlocker"

    @Volatile
    private var rules: AdBlockRules? = null

    private val _state = MutableStateFlow<AdBlockState?>(null)
    val state: StateFlow<AdBlockState?> = _state.asStateFlow()

    private val _updating = MutableStateFlow(false)
    val updating: StateFlow<Boolean> = _updating.asStateFlow()

    // Lists don't need the Cloudflare handling, nor a place in the network cache
    private val client by lazy { Injekt.get<NetworkHelper>().nonCloudflareClient.newBuilder().cache(null).build() }

    fun isBlocked(request: AdBlockRequest): Boolean = rules?.isBlocked(request) == true

    /** Blocking, call it from a background thread. */
    @Synchronized
    fun load(context: Context) {
        if (rules != null) return
        val start = SystemClock.elapsedRealtime()
        val compiled = updater(context).readRules()
        val loaded = compiled ?: AdBlockRules.EMPTY
        rules = loaded
        Log.i(
            TAG,
            "Loaded ${if (compiled != null) "the filter lists" else "no rules, the lists aren't downloaded yet"}: " +
                "${loaded.blocks.size} blocked hosts, ${loaded.exceptions.size} exception hosts, " +
                "${(loaded.blocks.byteSize + loaded.exceptions.byteSize) / 1024} KiB of tables, " +
                "in ${SystemClock.elapsedRealtime() - start} ms",
        )
    }

    /** Blocking. */
    fun readState(context: Context): AdBlockState =
        updater(context).readState().also { _state.value = it }

    /** Downloads the [lists] that changed and swaps the rules in use. Blocking. */
    fun update(context: Context, lists: List<FilterList>): AdBlockUpdater.Result {
        _updating.value = true
        try {
            val result = updater(context).update(lists)
            result.rules?.let { newRules ->
                synchronized(this) {
                    // Not loaded yet: the next load reads them
                    if (rules != null) rules = newRules
                }
            }
            _state.value = result.state
            return result
        } finally {
            _updating.value = false
        }
    }

    private fun updater(context: Context) = AdBlockUpdater(File(context.filesDir, "adblock"), client)
}
