package com.sf.animescraper.network.scraping.dto.details

interface AnimeDetails{
    var title : String
    var url : String
    var thumbnail_url : String?
    var release : String?
    var genre : List<String>?
    var description : String?
    var status : String?
    var favorite : Boolean

    companion object{
        fun create() : AnimeDetails {
            return AnimeDetailsImpl()
        }
    }
}