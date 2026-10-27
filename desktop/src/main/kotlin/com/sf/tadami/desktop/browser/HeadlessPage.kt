package com.sf.tadami.desktop.browser

import com.sf.tadami.network.AppCookieJar
import okhttp3.HttpUrl
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.handler.CefRequestHandler
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.handler.CefResourceRequestHandler
import org.cef.handler.CefResourceRequestHandlerAdapter
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * A page of the embedded browser that nobody sees, as the WebViews the Android app creates to solve a
 * challenge or run a page: it acts as the browser [userAgent] names, and sends [headers] with the pages it
 * navigates to. Blocking: never on the UI thread.
 *
 * @param onLoaded called on a thread of the page each time a document finished loading in the main frame
 * @param onRequest called from the browser's IO thread for every request the page makes
 */
internal class HeadlessPage(
    userAgent: String,
    private val headers: Map<String, String> = emptyMap(),
    private val onLoaded: ((url: String, status: Int) -> Unit)? = null,
    private val onRequest: ((url: String) -> Unit)? = null,
) : Closeable {

    private val client: CefClient = EmbeddedBrowser.app().createClient()

    /** Where the callbacks run: the browser's own threads must not wait on the DevTools calls they make. */
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "HeadlessPage").apply { isDaemon = true } }

    private val created = CountDownLatch(1)

    @Volatile
    var rendererGone = false
        private set

    val browser: CefBrowser

    init {
        client.addLifeSpanHandler(object : CefLifeSpanHandlerAdapter() {
            override fun onAfterCreated(browser: CefBrowser) {
                created.countDown()
            }

            // Popups (ads mostly) don't open
            override fun onBeforePopup(browser: CefBrowser, frame: CefFrame, targetUrl: String?, targetFrameName: String?) = true
        })
        client.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                val url = frame.url
                if (frame.isMain && url != BLANK) worker.execute { onLoaded?.invoke(url, httpStatusCode) }
            }
        })
        val resourceHandler = object : CefResourceRequestHandlerAdapter() {
            override fun onBeforeResourceLoad(browser: CefBrowser, frame: CefFrame?, request: CefRequest): Boolean {
                if (request.resourceType == CefRequest.ResourceType.RT_MAIN_FRAME) {
                    headers.forEach { (name, value) -> request.setHeaderByName(name, value, true) }
                }
                onRequest?.invoke(request.url)
                return false
            }
        }
        client.addRequestHandler(object : CefRequestHandlerAdapter() {
            override fun getResourceRequestHandler(
                browser: CefBrowser,
                frame: CefFrame?,
                request: CefRequest,
                isNavigation: Boolean,
                isDownload: Boolean,
                requestInitiator: String?,
                disableDefaultHandling: BoolRef,
            ): CefResourceRequestHandler = resourceHandler

            override fun onRenderProcessTerminated(
                browser: CefBrowser,
                status: CefRequestHandler.TerminationStatus,
                errorCode: Int,
                errorString: String?,
            ) {
                rendererGone = true
            }
        })
        browser = client.createBrowser(BLANK, true, false)
        browser.createImmediately()
        try {
            if (!created.await(START_TIMEOUT_SECONDS, TimeUnit.SECONDS)) throw IOException("The browser didn't start")
            browser.emulate(userAgent, screen = true)
        } catch (e: Exception) {
            close()
            throw e as? IOException ?: IOException("The browser didn't start", e)
        }
    }

    /** Navigates to [url] with the cookies the OkHttp clients have for it. */
    fun load(url: HttpUrl, jar: AppCookieJar) {
        runCatching { browser.pushCookies(url, jar) }
        browser.loadURL(url.toString())
    }

    override fun close() {
        worker.shutdownNow()
        browser.stopLoad()
        browser.close(true)
        client.dispose()
    }

    private companion object {
        const val BLANK = "about:blank"
        const val START_TIMEOUT_SECONDS = 20L
    }
}
