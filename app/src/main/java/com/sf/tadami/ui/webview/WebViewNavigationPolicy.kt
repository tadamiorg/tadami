package com.sf.tadami.ui.webview

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** What the WebView screen does with a main-frame navigation. */
enum class NavigationDecision {
    /** The window loads it. */
    ALLOW,

    /** The window stays on its page, the target opens in a popup window above it. */
    POPUP,

    /** Dropped. */
    BLOCK,
}

/**
 * Keeps the page the screen opened where it is. The ad scripts of embedded players navigate the whole
 * page to their sites (`top.location`, a `target=_top` link or form) instead of opening a popup, and
 * going back to the page runs them again.
 */
object WebViewNavigationPolicy {

    /** Popups stacked above the page at most, so an ad chain can't open windows forever. */
    const val MAX_POPUPS = 5

    /**
     * Decides for a main-frame navigation of a window to an http(s) [targetUrl], the screen handles
     * the other schemes.
     *
     * @param currentUrl the page the window shows
     * @param initialLoadDone whether the window's page finished its first load
     * @param canOpenPopup whether another popup fits on the stack
     * @param isAd whether the ad blocker blocks the target, as a page of the window or ([asPopup]) as
     * a page opening in a new window
     * @param siteOf the site of a URL, its registrable domain (eTLD+1) by default
     */
    fun decide(
        currentUrl: String?,
        targetUrl: String,
        isRedirect: Boolean,
        initialLoadDone: Boolean,
        isPopup: Boolean,
        canOpenPopup: Boolean,
        isAd: (target: HttpUrl, asPopup: Boolean) -> Boolean,
        siteOf: (HttpUrl) -> String = ::registrableDomain,
    ): NavigationDecision {
        val target = targetUrl.toHttpUrlOrNull() ?: return NavigationDecision.BLOCK
        // Other sites are what popups are for
        if (isPopup) return if (isAd(target, true)) NavigationDecision.BLOCK else NavigationDecision.ALLOW
        val current = currentUrl?.toHttpUrlOrNull()
        // The URL the screen opened and its redirects (sources move to mirror domains), the redirects
        // of the navigations allowed, then the page's own site
        if (!initialLoadDone || isRedirect || current == null || siteOf(current) == siteOf(target)) {
            return if (isAd(target, false)) NavigationDecision.BLOCK else NavigationDecision.ALLOW
        }
        // Would open in a new window: the popup rules apply
        if (isAd(target, true)) return NavigationDecision.BLOCK
        return if (canOpenPopup) NavigationDecision.POPUP else NavigationDecision.BLOCK
    }

    /** The registrable domain (eTLD+1), or the host for an IP address or a single label one. */
    private fun registrableDomain(url: HttpUrl): String = url.topPrivateDomain() ?: url.host
}
