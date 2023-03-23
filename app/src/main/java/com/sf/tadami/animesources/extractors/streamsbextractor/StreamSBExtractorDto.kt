package com.sf.tadami.animesources.extractors.streamsbextractor

import kotlinx.serialization.Serializable

@Serializable
data class Response(
    val stream_data: ResponseObject
) {
    @Serializable
    data class ResponseObject(
        val file: String,
    )
}