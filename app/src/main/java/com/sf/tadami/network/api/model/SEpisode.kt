package com.sf.tadami.network.api.model

interface SEpisode {
    var url : String
    var name : String
    var episodeNumber : Float
    var dateUpload : Long
    var languages: String?

    companion object{
        fun create() : SEpisode{
            return SEpisodeImpl()
        }
    }
}