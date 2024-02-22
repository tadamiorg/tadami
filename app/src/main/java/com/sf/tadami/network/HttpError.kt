package com.sf.tadami.network


sealed class HttpError : Exception() {
    data class Failure(val statusCode: Int?) : HttpError()
    data class CloudflareError(val msg: String) : HttpError()
}