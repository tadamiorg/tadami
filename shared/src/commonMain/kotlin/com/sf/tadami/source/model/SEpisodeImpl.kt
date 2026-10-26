package com.sf.tadami.source.model

class SEpisodeImpl : SEpisode {
    override lateinit var url: String
    override lateinit var name: String
    override var episodeNumber: Float = -1f
    override var dateUpload: Long = 0
    override var languages: String? = null
}