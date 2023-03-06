package com.sf.animescraper.network.scraping.dto.details

class AnimeDetailsImpl : AnimeDetails {
    override lateinit var title: String
    override lateinit var url: String
    override var thumbnail_url: String? = null
    override var release: String? = null
    override var genre: List<String>? = null
    override var description: String? = null
    override var status: String? = null
    override var favorite: Boolean = false
}