package com.sf.tadami.animesources.extractors.streamsbextractor

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Response(
    @SerialName("stream_data") val stream_data: ResponseObject
) {
    @Serializable
    data class ResponseObject(
        @SerialName("file") val file: String,
    )
}