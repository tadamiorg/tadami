package com.sf.animescraper.network.api.model

interface SAnime{
    var url : String
    var title : String
    var thumbnailUrl : String?
    var release : String?
    var status : String?
    var description : String?
    var genres : List<String>?
    var initialized: Boolean

    companion object{
        fun create() : SAnimeImpl {
            return SAnimeImpl()
        }
    }
}