package com.sf.tadami.network.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import java.net.URL

class ImageLoaderInterceptor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val protocol = if(request.url.isHttps) "https://" else "http://"
        val url = protocol + request.url.host
        val newRequest = request
            .newBuilder()
            .addHeader(
                "referer", url
            )
            .build()
        return chain.proceed(newRequest)
    }

    private fun extractDomain(url: String): String {
        val urlObject = URL(url)
        val protocol = urlObject.protocol // Get the protocol (e.g., "https")
        val domain = urlObject.host // Get the host/domain name
        return "$protocol://$domain"
    }
}