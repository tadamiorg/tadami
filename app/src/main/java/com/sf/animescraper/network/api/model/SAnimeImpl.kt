package com.sf.animescraper.network.api.model

class SAnimeImpl : SAnime {
    override lateinit var title: String
    override lateinit var url: String
    override var thumbnailUrl: String? = null
    override var release: String? = null
    override var status: String? = null
    override var description : String? = null
    override var genres: List<String>? = null
    override var initialized: Boolean = false
}
