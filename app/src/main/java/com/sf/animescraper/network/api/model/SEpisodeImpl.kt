package com.sf.animescraper.network.api.model

class SEpisodeImpl : SEpisode {
    override lateinit var url: String
    override lateinit var name: String
    override var episodeNumber: Float = -1f
    override var date: String? = null
}