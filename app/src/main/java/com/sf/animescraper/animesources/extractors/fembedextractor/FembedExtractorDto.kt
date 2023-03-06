package com.sf.animescraper.animesources.extractors.fembedextractor

import kotlinx.serialization.Serializable

@Serializable
data class FembedResponse(
    val success: Boolean,
    val data: List<FembedVideo> = emptyList()
)

@Serializable
data class FembedVideo(
    val file: String,
    val label: String
)
