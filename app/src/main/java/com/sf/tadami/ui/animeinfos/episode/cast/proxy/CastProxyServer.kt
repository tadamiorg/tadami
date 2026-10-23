package com.sf.tadami.ui.animeinfos.episode.cast.proxy

import android.content.Context
import android.util.Log
import com.sf.tadami.data.download.EpisodePreviews
import com.sf.tadami.network.player.HlsSegmentIvs
import com.sf.tadami.ui.animeinfos.episode.cast.getLocalIPAddress
import com.sf.tadami.ui.animeinfos.episode.cast.proxy.CastProxyServer.Companion.DEFAULT_PORT
import fi.iki.elonen.NanoHTTPD
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.util.Collections
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Local HTTP proxy that lets the *web* cast receiver play streams needing custom headers
 * (Referer, User-Agent, ...) that browsers cannot set on media requests.
 *
 * Stateless forwarder: the headers travel WITH the request (the receiver builds them into the
 * `headers` query parameter from the stream source it holds) and nothing is registered phone-side.
 * Payloads stream through untouched, with one exception: HLS media playlists whose segments
 * players would decrypt with the wrong IV get explicit IVs (ProxyPlaylists, HlsSegmentIvs),
 * a fix that depends on the playlist alone. The upstream request is the incoming
 * request's own headers overlaid with the carried ones ([buildUpstreamHeaders]); the response is
 * streamed back with its headers forwarded ([forwardedResponseHeaders]) plus permissive CORS.
 * Broken relative-URI resolution inside proxied manifests is the RECEIVER's job (it detects the
 * proxy origin in a request URL and re-bases it on the last manifest URL).
 *
 * Endpoints:
 * - `GET|HEAD /proxy?url=<urlencoded-absolute>[&headers=<urlencoded-json-object>][&type=<enc-mime>]`
 *   — streams the upstream response, mirroring the method (HEAD stays HEAD upstream — Shaka's
 *   container probe); `type` overrides the response Content-Type (the receiver replaces decoy CDN
 *   types — segments disguised as images — for that probe).
 * - `GET /echo?body=<enc>&type=<enc>` — reflects a receiver-composed body back verbatim.
 * - `GET /health` — plain "ok", for LAN reachability checks.
 * - `GET|HEAD /local-<token>-<name>` — the downloaded episode being cast, read from the phone's
 *   storage ([LocalMediaRoute]): its files with Range support, HLS playlists over them, and the
 *   file of its seekbar preview frames for the TV app ([LocalMedia.PREVIEWS_NAME], 404 without). The
 *   only phone-side state, set by [serveLocal]. The receiver wraps these http URLs in `/proxy`
 *   like any other: such a request is answered here directly, not fetched back over the network.
 *
 * Every response is `Connection: close` — NanoHTTPD keep-alive reuse stalls the receiver's
 * sequential startup requests in 5s read-timeout quanta, while LAN reconnects are sub-millisecond.
 *
 * Owned by CastControlService (started/stopped with it); the phone-side load path reaches it
 * through [CastProxyHolder] for `baseUrl()`.
 */
class CastProxyServer(
    private val appContext: Context,
    private val client: OkHttpClient,
    localFiles: LocalFileOpener = ContentFileOpener(appContext),
) {

    private val localRoute = LocalMediaRoute(localFiles)

    /** AdvancedPreferences User-Agent, applied only when neither the request nor the carried headers set one. */
    @Volatile
    var fallbackUserAgent: String = ""

    @Volatile
    private var server: Server? = null

    /** The bound port, or null when not running. */
    internal val listeningPort: Int?
        get() = server?.takeIf { it.isAlive }?.listeningPort

    /** `http://<lanIp>:<port>`, or null when not running / no LAN address (recomputed per call — the IP can change). */
    fun baseUrl(): String? {
        val port = listeningPort ?: return null
        val ip = getLocalIPAddress(appContext) ?: return null
        return "http://$ip:$port"
    }

    /** Binds [DEFAULT_PORT], falling back to an ephemeral port. Safe to call when already running. */
    @Synchronized
    fun startSafely(): Boolean {
        if (server?.isAlive == true) return true
        for (port in intArrayOf(DEFAULT_PORT, 0)) {
            try {
                server = Server(port).apply {
                    setAsyncRunner(BoundedAsyncRunner())
                    start(NanoHTTPD.SOCKET_READ_TIMEOUT, true)
                }
                Log.d(TAG, "Proxy listening on port ${server?.listeningPort}")
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Proxy failed to bind port $port", e)
                server = null
            }
        }
        return false
    }

    @Synchronized
    fun stop() {
        server?.stop()
        server = null
        localRoute.clear()
    }

    /**
     * Serves the downloaded episode [media] instead of the one served before. Returns the path
     * prefix of its URLs, `/local-<token>-`, to put after [baseUrl]: the same for the same
     * [LocalMedia.key], another random token for another episode.
     */
    fun serveLocal(media: LocalMedia): String = localRoute.serve(media)

    /**
     * The downloaded file behind [url], one of the local route's URLs of the episode served (the
     * seekbar preview decodes the file rather than its playlist over the network); null otherwise.
     */
    fun localFileUri(url: String): String? = url.toHttpUrlOrNull()?.encodedPath?.let(localRoute::fileUri)

    /** The seekbar preview frames of the downloaded episode behind [url], as [localFileUri]; null otherwise. */
    fun localPreviews(url: String): EpisodePreviews? = url.toHttpUrlOrNull()?.encodedPath?.let(localRoute::previews)

    private inner class Server(port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response = this@CastProxyServer.route(session)

        // Local files are served with their exact length, and a HEAD with no body
        override fun useGzipWhenAccepted(r: Response): Boolean = r !is LocalResponse && super.useGzipWhenAccepted(r)
    }

    private fun route(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        // One REQ/RESP pair per request: `adb logcat -s CastProxyServer` to follow a cast session.
        // Header NAMES only, values can hold tokens/cookies (Range is the one useful exception).
        val range = session.headers["range"]
        Log.d(
            TAG,
            "REQ ${session.method} ${session.uri}" +
                (range?.let { " range=$it" } ?: "") +
                " hdrs=${session.headers.keys}",
        )
        val response = try {
            if (session.method == NanoHTTPD.Method.OPTIONS) {
                cors(newResponse(NanoHTTPD.Response.Status.NO_CONTENT, "text/plain", ""))
            } else {
                when {
                    session.uri == "/health" -> cors(newResponse(NanoHTTPD.Response.Status.OK, "text/plain", "ok"))
                    session.uri == "/echo" -> handleEcho(session)
                    session.uri == "/proxy" -> handleProxy(session)
                    localRoute.handles(session.uri) -> cors(localRoute.respond(session.method, session.uri, range))
                    else -> cors(newResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", "not found"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "FAIL ${session.uri}", e)
            cors(newResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "text/plain", "proxy error"))
        }
        // No keep-alive, by design: Chrome's reuse of NanoHTTPD sockets stalls playback startup
        // in exact SOCKET_READ_TIMEOUT (5s) quanta — the next sequential request only got served
        // after the read-timeout closed the connection and the browser retried on a fresh one.
        // A new LAN TCP connection per request costs well under a millisecond.
        response.closeConnection(true)
        Log.d(TAG, "RESP ${response.status.requestStatus} type=${response.mimeType} ${session.uri}")
        return response
    }

    private fun handleProxy(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        // NanoHTTPD already url-decodes query parameters.
        val url = session.parameters["url"]?.firstOrNull()
            ?: return cors(newResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "text/plain", "missing url"))
        val httpUrl = url.toHttpUrlOrNull()
            ?: return cors(newResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "text/plain", "bad url"))
        // The web receiver wraps any http:// URL, ours too: a local file is read here, not fetched
        // back through the network (and a second worker thread)
        if (isOwnUrl(httpUrl, session.headers["host"]) && localRoute.handles(httpUrl.encodedPath)) {
            return cors(localRoute.respond(session.method, httpUrl.encodedPath, session.headers["range"]))
        }
        val carriedHeaders = parseHeadersParam(session.parameters["headers"]?.firstOrNull())

        // Mirror the incoming method: Shaka's container probe uses HEAD, and answering it with a
        // full upstream GET downloads whole segments just to discard them (and wedges the
        // connection on the unread body). Anything unexpected falls back to GET.
        val method = if (session.method == NanoHTTPD.Method.HEAD) "HEAD" else "GET"
        val rangeRequested = session.headers["range"] != null

        val headers = buildUpstreamHeaders(session.headers, carriedHeaders, fallbackUserAgent).let {
            // A playlist may need fixing (ProxyPlaylists): get it decoded
            if (method == "GET" && !rangeRequested && isPlaylistUrl(httpUrl)) withoutAcceptEncoding(it) else it
        }
        // Logged BEFORE the upstream call so a hung/failed fetch still shows its target.
        // Header NAMES only (values can hold tokens/cookies).
        Log.d(TAG, "HDR carried=${carriedHeaders != null} keys=${headers.names()} url=$url")

        val upstream = client.newCall(
            Request.Builder().url(httpUrl).headers(headers).method(method, null).build(),
        ).execute()
        val body = upstream.body
        // Caller-chosen response Content-Type override (`type` param): anime CDNs disguise media
        // segments as images, and the decoy type poisons the player's container probe. The caller
        // (web receiver) decides — the proxy never sniffs.
        val contentType = session.parameters["type"]?.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: upstream.header("Content-Type")
            ?: "application/octet-stream"
        val length = body.contentLength()
        val forwarded = forwardedResponseHeaders(upstream.headers)
        Log.d(TAG, "UP ${upstream.code} type=$contentType len=$length fwd=${forwarded.map { it.first }} url=$url")

        val fixablePlaylist = isWholeDecodedBody(
            method = method,
            rangeRequested = rangeRequested,
            code = upstream.code,
            contentEncoding = upstream.header("Content-Encoding"),
            contentLength = length,
        ) && startsLikePlaylist(upstream.peekBody(PLAYLIST_MAGIC_BYTES).bytes())
        if (fixablePlaylist) {
            val original = body.string()
            val fixed = HlsSegmentIvs.fix(original)
            if (fixed != null) Log.d(TAG, "FIX playlist segment IVs url=$url")
            val bytes = (fixed ?: original).toByteArray(Charsets.UTF_8)
            val response = NanoHTTPD.newFixedLengthResponse(
                statusOf(upstream.code), contentType, ByteArrayInputStream(bytes), bytes.size.toLong(),
            )
            for ((name, value) in playlistResponseHeaders(forwarded, rewritten = fixed != null)) {
                response.addHeader(name, value)
            }
            return cors(response)
        }

        val response = if (length >= 0) {
            NanoHTTPD.newFixedLengthResponse(statusOf(upstream.code), contentType, body.byteStream(), length)
        } else {
            NanoHTTPD.newChunkedResponse(statusOf(upstream.code), contentType, body.byteStream())
        }
        for ((name, value) in forwarded) {
            response.addHeader(name, value)
        }
        return cors(response)
    }

    /**
     * `GET /echo?body=<enc>&type=<enc>` — reflects a receiver-composed body back with the given
     * Content-Type (+ CORS). Pure reflection, no parsing: it lets the receiver serve itself
     * content over plain http (e.g. its synthetic HLS master) without the proxy knowing or
     * caring what it is.
     */
    private fun handleEcho(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val body = session.parameters["body"]?.firstOrNull()
            ?: return cors(newResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "text/plain", "missing body"))
        val type = session.parameters["type"]?.firstOrNull() ?: "text/plain"
        Log.d(TAG, "ECHO ${body.length} chars type=$type")
        return cors(newResponse(NanoHTTPD.Response.Status.OK, type, body))
    }

    /** Whether [url] points at this very server, as the client reached it ([host], its Host header). */
    private fun isOwnUrl(url: HttpUrl, host: String?): Boolean =
        host != null && url.port == listeningPort && "${url.host}:${url.port}".equals(host, ignoreCase = true)

    private fun newResponse(status: NanoHTTPD.Response.IStatus, mime: String, text: String): NanoHTTPD.Response =
        NanoHTTPD.newFixedLengthResponse(status, mime, text)

    private fun statusOf(code: Int): NanoHTTPD.Response.IStatus =
        NanoHTTPD.Response.Status.lookup(code) ?: object : NanoHTTPD.Response.IStatus {
            override fun getDescription(): String = "$code Upstream Status"
            override fun getRequestStatus(): Int = code
        }

    /** Browsers enforce CORS on the receiver's media/subtitle fetches; answer permissively everywhere. */
    private fun cors(response: NanoHTTPD.Response): NanoHTTPD.Response = response.apply {
        addHeader("Access-Control-Allow-Origin", "*")
        addHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
        addHeader("Access-Control-Allow-Headers", "*")
        addHeader("Access-Control-Expose-Headers", "*")
    }

    /** NanoHTTPD's default runner spawns unbounded threads; cap them, playback needs only a few. */
    private class BoundedAsyncRunner : NanoHTTPD.AsyncRunner {
        // Core size = max: with an unbounded queue the pool never grows past its core size, so a
        // core of 0 served one connection at a time (a streaming GET blocked subtitles and seeks).
        private val executor = ThreadPoolExecutor(
            MAX_THREADS, MAX_THREADS, 30L, TimeUnit.SECONDS, LinkedBlockingQueue(),
        ) { runnable ->
            Thread(runnable, "CastProxy-worker").apply { isDaemon = true }
        }.apply { allowCoreThreadTimeOut(true) }
        private val running = Collections.synchronizedList(mutableListOf<NanoHTTPD.ClientHandler>())

        override fun exec(code: NanoHTTPD.ClientHandler) {
            running.add(code)
            executor.execute {
                try {
                    code.run()
                } finally {
                    running.remove(code)
                }
            }
        }

        override fun closed(clientHandler: NanoHTTPD.ClientHandler) {
            running.remove(clientHandler)
        }

        override fun closeAll() {
            synchronized(running) { running.toList() }.forEach { it.close() }
            executor.shutdownNow()
        }

        companion object {
            private const val MAX_THREADS = 16
        }
    }

    companion object {
        const val DEFAULT_PORT = 8214
        private const val TAG = "CastProxyServer"
    }
}

/** Bridge so the phone-side load path (EpisodeActivity) reaches the service-owned proxy for baseUrl(). */
object CastProxyHolder {
    @Volatile
    var server: CastProxyServer? = null
}
