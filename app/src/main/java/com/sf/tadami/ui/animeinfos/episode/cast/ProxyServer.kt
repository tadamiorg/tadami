package com.sf.tadami.ui.animeinfos.episode.cast

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.http4k.core.*
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.KtorCIO
import org.http4k.server.asServer
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ProxyServer {
    private var proxyServer: Http4kServer? = null
    private val json: Json = Injekt.get()

    private val logFilter: Filter = Filter { next ->
        { request ->
            Log.e("Request made",request.uri.toString())
            next(request)
        }
    }

    private var requestClient : OkHttpClient? = OkHttpClient.Builder().build()

    private val app = ServerFilters.RequestTracing()
        .then(
            routes(
                "/" bind Method.GET to { request: Request ->
                    val originalHeaders =
                        request.headers.filter { (key, _) -> key.lowercase() != "accept-language" }
                    val url = request.query("url")

                    val sourceHeadersString = request.query("headers")
                    val sourceHeaders = okhttp3.Headers.Builder()
                    if (sourceHeadersString != null) {
                        val parsedHeaders =
                            json.parseToJsonElement(sourceHeadersString.toString()).jsonObject

                        parsedHeaders.forEach { (key, value) ->
                            sourceHeaders.add(key, value.jsonPrimitive.content)
                        }

                        originalHeaders.forEach { (key, value) ->
                            if (sourceHeaders[key] == null && sourceHeaders[key.lowercase()] == null) {
                                if (key.lowercase() == "host") {
                                    sourceHeaders.add(key, url!!.toHttpUrl().host)
                                } else {
                                    sourceHeaders.add(key, value.toString())
                                }
                            }
                        }
                    } else {
                        originalHeaders.forEach { (key, value) ->
                            if (key.lowercase() == "host") {
                                sourceHeaders.add(key, url!!.toHttpUrl().host)
                            } else {
                                sourceHeaders.add(key, value.toString())
                            }

                        }
                    }

                    val videoRequest = okhttp3.Request.Builder().url(url.toString()).headers(sourceHeaders.build()).build()
                    if(requestClient != null){
                        val videoResponse = requestClient!!.newCall(videoRequest).execute()
                        val streamBody = StreamBody(videoResponse.body.byteStream())
                        Response(Status(videoResponse.code,videoResponse.message))
                            .headers(videoResponse.headers.toList())
                            .body(streamBody)
                    }else{
                        Response(Status.GONE)
                    }

                },
                "/{path:.*}" bind Method.GET to {
                    Response(Status.NOT_FOUND)
                }
            )
        )

    fun isRunning() = proxyServer != null

    fun start(port: Int = 8000) {
        if (proxyServer != null) return
        proxyServer = ServerFilters.CatchAll()
            .then(logFilter)
            .then(app)
            .asServer(KtorCIO(port))
            .start()
    }

    fun stop() {
        if (proxyServer == null) return
        requestClient = null
        proxyServer!!.stop()
        proxyServer = null
    }
}
