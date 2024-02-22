package com.sf.tadami.source.model

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
        fun create() : SAnime {
            return SAnimeImpl()
        }
    }
}