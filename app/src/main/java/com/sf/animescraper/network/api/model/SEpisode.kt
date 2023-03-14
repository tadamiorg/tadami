package com.sf.animescraper.network.api.model

interface SEpisode {
    var url : String
    var name : String
    var episodeNumber : Float
    var dateUpload : Long

    companion object{
        fun create() : SEpisode{
            return SEpisodeImpl()
        }
    }
}