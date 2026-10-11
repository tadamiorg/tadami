package com.sf.tadami.network.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.annotation.MainThread
import androidx.webkit.UserAgentMetadata
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

object WebViewUtil {
    const val MINIMUM_WEBVIEW_VERSION = 114

    @Volatile
    private var engineMajorVersion: Int? = null

    /**
     * Chromium major version of the installed WebView, which the default user agent announces.
     * Read from the provider's package, or from the engine itself when [detectVersion] ran at
     * startup for a provider numbering its versions its own way. Null when there is no WebView.
     */
    val webViewMajorVersion: Int?
        get() = packageMajorVersion ?: engineMajorVersion

    /** From the provider's package: needs neither a WebView instance nor the main thread. */
    private val packageMajorVersion: Int? by lazy {
        try {
            val webViewPackage = WebView.getCurrentWebViewPackage()
            UserAgents.chromiumMajorFromPackage(webViewPackage?.packageName, webViewPackage?.versionName)
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Checks at startup which Chromium version the device's WebView runs, so that the default
     * user agent matches it. Most providers number their package like Chromium, which is cheap to
     * read. For the others (e.g. Huawei's), the engine's own user agent is read, which loads the
     * WebView: call it on the main thread, before anything reads [defaultUserAgent].
     */
    @MainThread
    fun detectVersion(context: Context) {
        if (packageMajorVersion != null || engineMajorVersion != null) return
        engineMajorVersion = try {
            UserAgents.chromeMajor(WebSettings.getDefaultUserAgent(context))
        } catch (e: Throwable) {
            // No WebView, or it is being updated
            null
        }
    }

    /** Chrome for Android user agent matching the installed WebView engine version. */
    val defaultUserAgent: String by lazy {
        UserAgents.mobileChrome(webViewMajorVersion ?: UserAgents.FALLBACK_CHROME_MAJOR)
    }

    fun supportsWebView(context: Context): Boolean {
        try {
            // May throw android.webkit.WebViewFactory$MissingWebViewPackageException if WebView
            // is not installed
            CookieManager.getInstance()
        } catch (e: Throwable) {
            Log.e("WebViewUtil",e.stackTraceToString())
            return false
        }

        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_WEBVIEW)
    }

    fun isOutdated(): Boolean {
        val version = webViewMajorVersion ?: return false
        return version < MINIMUM_WEBVIEW_VERSION
    }

    fun getVersion(context: Context): String {
        val webView = WebView.getCurrentWebViewPackage() ?: return "how did you get here?"
        val pm = context.packageManager
        val label = webView.applicationInfo?.loadLabel(pm)
        val version = webView.versionName
        return "$label $version"
    }
}

@SuppressLint("SetJavaScriptEnabled")
fun WebView.setDefaultSettings() {
    with(settings) {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        useWideViewPort = true
        loadWithOverviewMode = true
        cacheMode = WebSettings.LOAD_DEFAULT

        // Allow zooming
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
    }
    // Challenge widgets (e.g. Cloudflare Turnstile) run in third-party iframes
    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
    // WebView tags its requests with an X-Requested-With header holding the app id, which no
    // browser sends and anti-bot challenges use to single WebViews out
    if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
        WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())
    }
}

/**
 * Uses [userAgent] for both the User-Agent header and the User-Agent Client Hints, so the browser
 * doesn't announce one Chrome version in the UA and another one in `Sec-CH-UA`. Anti-bot
 * challenges reject that mismatch, and `cf_clearance` is bound to the UA that solved it.
 */
/**
 * JavaScript evaluating to true while Cloudflare is still at work on the page: its challenge (or
 * error) page, or the site's page still running the precursor script that follows a challenge.
 * Both define `_cf_chl_opt`. The clearance the precursor step writes is rejected by HTTP clients
 * until that script is done, so it doesn't count as solved yet.
 */
internal const val CLOUDFLARE_CHALLENGE_PROBE =
    "(function () { return typeof window._cf_chl_opt !== 'undefined' || " +
        "document.getElementById('challenge-error-title') !== null || " +
        "document.getElementById('challenge-error-text') !== null; })()"

private const val CHROMIUM_BRAND = "Chromium"
private const val CHROME_BRAND = "Google Chrome"
private const val WEBVIEW_BRAND = "Android WebView"

fun WebView.applyUserAgent(userAgent: String) {
    // Chromium falls back to its default UA when given an empty one
    if (userAgent.isBlank()) return
    settings.userAgentString = userAgent

    if (!WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) return
    val hints = UserAgents.clientHintsFor(userAgent) ?: return
    try {
        val base = WebSettingsCompat.getUserAgentMetadata(settings)
        val engineMajor = base.brandVersionList.firstOrNull { it.brand == CHROMIUM_BRAND }?.majorVersion
        val uaMajor = hints.brands.firstOrNull { it.name == CHROMIUM_BRAND }?.majorVersion
        val android = hints.platform == "Android"
        // Same version as the engine: WebView's own brands already are Chrome's (GREASE brand,
        // order, full versions), except that it names itself instead of Chrome
        val keepEngineBrands = android && engineMajor == uaMajor &&
            base.brandVersionList.any { it.brand == WEBVIEW_BRAND }
        val brands = if (keepEngineBrands) {
            base.brandVersionList.map { brand ->
                if (brand.brand != WEBVIEW_BRAND) brand else UserAgentMetadata.BrandVersion.Builder()
                    .setBrand(CHROME_BRAND)
                    .setMajorVersion(brand.majorVersion)
                    .setFullVersion(brand.fullVersion)
                    .build()
            }
        } else {
            hints.brands.map {
                UserAgentMetadata.BrandVersion.Builder()
                    .setBrand(it.name)
                    .setMajorVersion(it.majorVersion)
                    .setFullVersion(it.fullVersion)
                    .build()
            }
        }
        val metadata = UserAgentMetadata.Builder(base)
            .setBrandVersionList(brands)
            .setFullVersion(if (keepEngineBrands) base.fullVersion else hints.fullVersion)
            .setPlatform(hints.platform)
            .setMobile(hints.mobile)
            .apply {
                // Chrome for Android reports the real Android version and device model, which its
                // reduced UA string hides, so keep the WebView's ones
                if (!android) setPlatformVersion(hints.platformVersion)
                if (!hints.mobile) {
                    setModel("")
                    setArchitecture(hints.architecture)
                    setBitness(hints.bitness)
                }
            }
            .build()
        WebSettingsCompat.setUserAgentMetadata(settings, metadata)
    } catch (e: Exception) {
        Log.w("WebViewUtil", "Could not align user agent client hints", e)
    }
}
