package com.sf.tadami.ui.animeinfos.episode.cast

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.StreamBody
import org.http4k.core.then
import org.http4k.filter.AllowAll
import org.http4k.filter.CorsPolicy
import org.http4k.filter.OriginPolicy
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.KtorCIO
import org.http4k.server.asServer
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class ProxyServer {
    private var proxyServer: Http4kServer? = null
    private val json: Json = Injekt.get()

    private val logFilter: Filter = Filter { next ->
        { request ->
            Log.d("Requests", request.uri.toString())
            next(request)
        }
    }

    private var requestClient: OkHttpClient? = OkHttpClient.Builder().build()

    private val app = routes(
        "/" bind Method.GET to { request: Request ->
            try {
                Log.d("Proxy", "Processing request: ${request.uri}")

                // Extract URL directly from the query string
                val urlParam = request.uri.query.split("&")
                    .firstOrNull { it.startsWith("url=") }
                    ?.substringAfter("url=")

                if (urlParam.isNullOrEmpty()) {
                    Log.d("Proxy", "URL parameter is missing")
                    return@to Response(Status.BAD_REQUEST).body("Missing 'url' parameter")
                }

                val decodedUrl = URLDecoder.decode(urlParam, StandardCharsets.UTF_8.name())
                Log.d("Proxy", "Decoded URL: $decodedUrl")

                // Extract headers parameter
                val headersParam = request.uri.query.split("&")
                    .firstOrNull { it.startsWith("headers=") }
                    ?.substringAfter("headers=")

                Log.d("Proxy", "Headers param: $headersParam")

                // Keep only the necessary headers from the original request
                val originalHeaders = request.headers.filter { (key, _) ->
                    key.lowercase() != "accept-language" &&
                            key.lowercase() != "host" &&
                            key.lowercase() != "origin"
                }

                val sourceHeaders = okhttp3.Headers.Builder()

                if (!headersParam.isNullOrEmpty()) {
                    try {
                        // Decode the headers parameter
                        val decodedHeadersParam = URLDecoder.decode(headersParam, StandardCharsets.UTF_8.name())
                        Log.d("Proxy", "Decoded headers: $decodedHeadersParam")
                        val parsedHeaders = json.parseToJsonElement(decodedHeadersParam).jsonObject

                        // Add headers from the parameter
                        parsedHeaders.forEach { (key, value) ->
                            sourceHeaders.add(key, value.jsonPrimitive.content)
                        }

                        // Add remaining headers from the original request if not already present
                        originalHeaders.forEach { (key, value) ->
                            if (sourceHeaders[key] == null && sourceHeaders[key.lowercase()] == null) {
                                if (value != null) {
                                    sourceHeaders.add(key, value)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.d("Proxy", "Headers parsing error: ${e.message}")
                        // In case of error parsing headers, fall back to using original headers
                        originalHeaders.forEach { (key, value) ->
                            if (value != null) {
                                sourceHeaders.add(key, value)
                            }
                        }
                    }
                } else {
                    // No headers parameter, use the original headers
                    originalHeaders.forEach { (key, value) ->
                        if (value != null) {
                            sourceHeaders.add(key, value)
                        }
                    }
                }

                // Make the actual request
                requestClient?.let { client ->
                    Log.d("Proxy", "Making request to: $decodedUrl")
                    try {
                        val videoRequest = okhttp3.Request.Builder()
                            .url(decodedUrl)
                            .headers(sourceHeaders.build())
                            .build()

                        val videoResponse = client.newCall(videoRequest).execute()
                        val streamBody = StreamBody(videoResponse.body.byteStream())

                        Log.d("Proxy", "Response received with status: ${videoResponse.code}")
                        return@to Response(Status(videoResponse.code, videoResponse.message))
                            .headers(videoResponse.headers.toList())
                            .body(streamBody)
                    } catch (e: Exception) {
                        Log.d("Proxy", "Request error: ${e.message}")
                        return@to Response(Status.INTERNAL_SERVER_ERROR)
                            .body("Error: ${e.message}")
                    }
                } ?: run {
                    Log.d("Proxy", "Request client is null")
                    Response(Status.GONE)
                }
            } catch (e: Exception) {
                Log.e("Proxy", "Unexpected error: ${e.message}")
                Response(Status.INTERNAL_SERVER_ERROR).body("Unexpected error: ${e.message}")
            }
        },
        "/{path:.*}" bind Method.GET to {
            Response(Status.NOT_FOUND)
        }
    )

    fun start(port: Int = 8000) {
        if (proxyServer != null) return
        proxyServer = ServerFilters.CatchAll()
            .then(logFilter)
            .then(
                ServerFilters.Cors.invoke(
                    CorsPolicy(
                        originPolicy = OriginPolicy.AllowAll(),
                        methods = listOf(Method.GET),
                        headers = listOf("*")
                    )
                )
            )
            .then(app)
            .asServer(KtorCIO(port))
            .start()

        Log.i("ProxyServer", "Started on port $port")
    }

    fun stop() {
        if (proxyServer == null) return
        requestClient = null
        proxyServer!!.stop()
        proxyServer = null
        Log.i("ProxyServer", "Stopped")
    }
}