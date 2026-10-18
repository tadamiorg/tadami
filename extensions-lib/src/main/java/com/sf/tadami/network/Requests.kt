package com.sf.tadami.network

import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.Request
import okhttp3.RequestBody

private val DEFAULT_CACHE_CONTROL : CacheControl = throw Exception("Stub!")
private val DEFAULT_HEADERS : Headers = throw Exception("Stub!")
private val DEFAULT_BODY: RequestBody = throw Exception("Stub!")

/**
 * A GET request. By default without headers, cached 10 minutes.
 *
 * @since extensions-lib 1.5
 */
fun GET(
    url: String,
    headers: Headers = DEFAULT_HEADERS,
    cache: CacheControl = DEFAULT_CACHE_CONTROL
): Request {
    throw Exception("Stub!")
}

/**
 * A HEAD request. By default without headers, cached 10 minutes.
 *
 * @since extensions-lib 1.5
 */
fun HEAD(
    url: String,
    headers: Headers = DEFAULT_HEADERS,
    cache: CacheControl = DEFAULT_CACHE_CONTROL
): Request {
    throw Exception("Stub!")
}

/**
 * A POST request of [body], by default an empty form.
 *
 * @since extensions-lib 1.5
 */
fun POST(
    url: String,
    headers: Headers = DEFAULT_HEADERS,
    body: RequestBody = DEFAULT_BODY,
    cache: CacheControl = DEFAULT_CACHE_CONTROL
): Request {
    throw Exception("Stub!")
}

/**
 * A PUT request of [body], by default an empty form.
 *
 * @since extensions-lib 1.7
 */
fun PUT(
    url: String,
    headers: Headers = DEFAULT_HEADERS,
    body: RequestBody = DEFAULT_BODY,
    cache: CacheControl = DEFAULT_CACHE_CONTROL,
): Request {
    throw Exception("Stub!")
}

/**
 * A DELETE request of [body], by default an empty form.
 *
 * @since extensions-lib 1.7
 */
fun DELETE(
    url: String,
    headers: Headers = DEFAULT_HEADERS,
    body: RequestBody = DEFAULT_BODY,
    cache: CacheControl = DEFAULT_CACHE_CONTROL,
): Request {
    throw Exception("Stub!")
}
