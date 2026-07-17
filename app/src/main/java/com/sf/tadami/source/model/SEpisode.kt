package com.sf.tadami.source.model

interface SEpisode {
    var url : String
    var name : String
    var episodeNumber : Float
    var dateUpload : Long
    var languages: String?
    var seasonName: String?
    var seasonNumber: Float?

    companion object{
        fun create() : SEpisode {
            return SEpisodeImpl()
        }
    }
}