package com.sf.tadami.network.interceptors


import okhttp3.Interceptor
import okhttp3.Response

class UserAgentInterceptor(
    private val userAgent: () -> String,
    /** Whether to replace the user agent a request already has, e.g. one the settings changed since */
    private val replaces: (String) -> Boolean = { false },
) : Interceptor {

    constructor(userAgent: String) : this({ userAgent })

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestUserAgent = originalRequest.header("User-Agent")

        return if (requestUserAgent.isNullOrEmpty() || replaces(requestUserAgent)) {
            val newRequest = originalRequest
                .newBuilder()
                .removeHeader("User-Agent")
                .addHeader("User-Agent", userAgent())
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}
