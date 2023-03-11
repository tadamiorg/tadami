package com.sf.animescraper.network.api.model

interface SEpisode {
    var url : String
    var name : String
    var episodeNumber : Float
    var date : String?

    companion object{
        fun create() : SEpisode{
            return SEpisodeImpl()
        }
    }
}