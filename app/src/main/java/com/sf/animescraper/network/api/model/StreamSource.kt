package com.sf.animescraper.network.api.model

import okhttp3.Headers

data class StreamSource(
    val url : String = "",
    val quality : String = "",
    val headers: Headers? = null
)