package com.sf.tadami.desktop.browser

import com.sf.tadami.desktop.AppDirs
import com.sf.tadami.network.AppCookieJar
import com.sf.tadami.network.DesktopBrowser
import com.sf.tadami.network.DesktopCookieJar
import com.sf.tadami.network.WebViewResolver
import com.sf.tadami.network.utils.UserAgents
import com.sf.tadami.utils.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import me.friwi.jcefmaven.CefAppBuilder
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.Interceptor
import org.cef.CefApp
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Chromium (JCEF), the desktop's WebView: the WebView screen, the Cloudflare challenges and the pages
 * the extensions run. Started on first use; its profile keeps the cookies and the site data across
 * launches, as Android's WebView does.
 *
 * The OkHttp clients keep their own cookie jar: the cookies of a URL are copied into the browser
 * before it loads it, and back once it has.
 */
object EmbeddedBrowser : DesktopBrowser {

    private const val TAG = "EmbeddedBrowser"

    @Volatile
    private var app: CefApp? = null

    /** Blocks the first time, while Chromium is unpacked (once) and started: never on the UI thread. */
    fun app(): CefApp = app ?: synchronized(this) {
        app ?: CefAppBuilder().run {
            setInstallDir(File(AppDirs.data, "jcef"))
            setProgressHandler { state, percent -> Log.i(TAG, "$state ${percent.toInt()}%") }
            val profile = File(AppDirs.data, "browser").absolutePath
            cefSettings.root_cache_path = profile
            cefSettings.cache_path = profile
            cefSettings.persist_session_cookies = true
            cefSettings.windowless_rendering_enabled = true
            cefSettings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_ERROR
            build()
        }.also { app = it }
    }

    val isStarted: Boolean get() = app != null

    fun shutdown() {
        app?.dispose()
    }

    override fun cloudflareInterceptor(userAgent: () -> String): Interceptor =
        BrowserCloudflareInterceptor(DesktopCookieJar, userAgent)

    override fun webViewResolver(userAgent: () -> String): WebViewResolver =
        BrowserWebViewResolver(DesktopCookieJar, userAgent)
}

private val json = Json { ignoreUnknownKeys = true }

private const val DEVTOOLS_TIMEOUT_SECONDS = 15L

/** Calls a method of the Chrome DevTools protocol on the page, from a thread other than the browser's. */
internal fun CefBrowser.devTools(method: String, params: JsonObject = JsonObject(emptyMap())): JsonObject {
    val client = devToolsClient ?: throw IOException("The browser isn't created yet")
    val result = try {
        client.executeDevToolsMethod(method, params.toString()).get(DEVTOOLS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    } catch (e: Exception) {
        throw IOException("$method failed", e)
    }
    return json.parseToJsonElement(result).jsonObject
}

/**
 * Evaluates [expression] in the main frame: its value as a JSON element (a string for a string), or
 * null when it throws or the page isn't there.
 */
internal fun CefBrowser.evaluate(expression: String, awaitPromise: Boolean = false): JsonElement? {
    val response = runCatching {
        devTools(
            "Runtime.evaluate",
            buildJsonObject {
                put("expression", expression)
                put("returnByValue", true)
                put("awaitPromise", awaitPromise)
            },
        )
    }.getOrElse { return null }
    if (response["exceptionDetails"] != null) return null
    return response["result"]?.jsonObject?.get("value") ?: JsonNull
}

/** As a string, what a JavaScript value is to the WebView of Android: strings as they are, the rest as JSON. */
internal fun JsonElement.asScriptResult(): String? = when (this) {
    is JsonNull -> null
    is JsonPrimitive -> content
    else -> toString()
}

internal fun JsonElement?.isTrue(): Boolean = (this as? JsonPrimitive)?.booleanOrNull == true

/**
 * Makes the page the browser the user agent names, as the WebView of Android does: the UA, the client
 * hints and the platform, and for a phone its touch, as Chrome's device mode does. A page nobody sees
 * gets a [screen] too: a phone's, or a window's instead of its 1x1 pixel. Cloudflare checks that they
 * agree, and binds its clearance to the UA.
 */
internal fun CefBrowser.emulate(userAgent: String, screen: Boolean) {
    if (userAgent.isBlank()) return
    val hints = UserAgents.clientHintsFor(userAgent)
    devTools(
        "Emulation.setUserAgentOverride",
        buildJsonObject {
            put("userAgent", userAgent)
            if (hints == null) return@buildJsonObject
            put(
                "platform",
                when (hints.platform) {
                    "Android" -> "Linux armv81"
                    "Windows" -> "Win32"
                    "macOS" -> "MacIntel"
                    else -> "Linux x86_64"
                },
            )
            putJsonObject("userAgentMetadata") {
                putJsonArray("brands") {
                    hints.brands.forEach { addJsonObject { put("brand", it.name); put("version", it.majorVersion) } }
                }
                putJsonArray("fullVersionList") {
                    hints.brands.forEach { addJsonObject { put("brand", it.name); put("version", it.fullVersion) } }
                }
                put("fullVersion", hints.fullVersion)
                put("platform", hints.platform)
                put("platformVersion", hints.platformVersion)
                put("architecture", hints.architecture)
                put("model", "")
                put("mobile", hints.mobile)
                put("bitness", if (hints.bitness > 0) hints.bitness.toString() else "")
            }
        },
    )
    val mobile = hints?.mobile == true
    if (screen) {
        devTools(
            "Emulation.setDeviceMetricsOverride",
            buildJsonObject {
                put("width", if (mobile) 412 else 1280)
                put("height", if (mobile) 915 else 800)
                put("deviceScaleFactor", if (mobile) 2.625 else 1.0)
                put("mobile", mobile)
            },
        )
    }
    if (!mobile) return
    devTools(
        "Emulation.setTouchEmulationEnabled",
        buildJsonObject {
            put("enabled", true)
            put("maxTouchPoints", 5)
        },
    )
}

/** The browser's cookies for [url], httpOnly ones included. */
internal fun CefBrowser.cookies(url: HttpUrl): List<Cookie> {
    val response = devTools(
        "Network.getCookies",
        buildJsonObject { putJsonArray("urls") { add(JsonPrimitive(url.toString())) } },
    )
    return response["cookies"]?.jsonArray.orEmpty().mapNotNull { element ->
        val cookie = element.jsonObject
        val domain = cookie["domain"]?.jsonPrimitive?.content ?: return@mapNotNull null
        runCatching {
            Cookie.Builder()
                .name(cookie["name"]!!.jsonPrimitive.content)
                .value(cookie["value"]!!.jsonPrimitive.content)
                .path(cookie["path"]?.jsonPrimitive?.content ?: "/")
                .apply {
                    if (domain.startsWith(".")) domain(domain.removePrefix(".")) else hostOnlyDomain(domain)
                    if (cookie["secure"].isTrue()) secure()
                    if (cookie["httpOnly"].isTrue()) httpOnly()
                    val expires = cookie["expires"]?.jsonPrimitive?.doubleOrNull ?: -1.0
                    if (!cookie["session"].isTrue() && expires > 0) expiresAt((expires * 1000).toLong())
                }
                .build()
        }.getOrNull()
    }
}

/** Gives the browser the cookies the OkHttp clients have for [url], before it loads it. */
internal fun CefBrowser.pushCookies(url: HttpUrl, jar: AppCookieJar) {
    val cookies = jar.get(url).takeIf { it.isNotEmpty() } ?: return
    devTools(
        "Network.setCookies",
        buildJsonObject {
            putJsonArray("cookies") {
                cookies.forEach { cookie ->
                    addJsonObject {
                        put("name", cookie.name)
                        put("value", cookie.value)
                        put("path", cookie.path)
                        if (cookie.hostOnly) {
                            put("url", "${url.scheme}://${cookie.domain}${cookie.path}")
                        } else {
                            put("domain", ".${cookie.domain}")
                        }
                        put("secure", cookie.secure)
                        put("httpOnly", cookie.httpOnly)
                        if (cookie.persistent) put("expires", cookie.expiresAt / 1000.0)
                    }
                }
            }
        },
    )
}

/** Gives the OkHttp clients the cookies the browser has for [url] (a Cloudflare clearance...). */
internal fun CefBrowser.pullCookies(url: HttpUrl, jar: AppCookieJar) {
    val cookies = cookies(url)
    if (cookies.isNotEmpty()) jar.saveFromResponse(url, cookies)
}

/** Forgets the cookies of [url], the browser's and the OkHttp clients' ones. */
internal fun CefBrowser.clearCookies(url: HttpUrl, jar: AppCookieJar) {
    cookies(url).forEach { cookie ->
        devTools(
            "Network.deleteCookies",
            buildJsonObject {
                put("name", cookie.name)
                put("domain", if (cookie.hostOnly) cookie.domain else ".${cookie.domain}")
                put("path", cookie.path)
            },
        )
    }
    jar.remove(url, null, 0)
}
