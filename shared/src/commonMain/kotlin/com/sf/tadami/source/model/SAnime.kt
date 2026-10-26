package com.sf.tadami.source.model

interface SAnime{
    var url : String
    var title : String
    /**
     * Original/canonical site title used for search & migration. Extensions that rewrite [title]
     * (e.g. by appending a season name) should set this to the untouched title. When null,
     * consumers fall back to [title].
     */
    var rawTitle : String?
    /**
     * Language of this entry when the site lists each language as its own anime (e.g. "VF",
     * "VOSTFR"). Shown as a badge next to the title on every screen, so keep it out of [title].
     * Optional, needs extensions lib 1.6.
     */
    var language : String?
    var thumbnailUrl : String?
    var release : String?
    var studio : String?
    var author : String?
    var status : SAnimeStatus
    var description : String?
    var genres : List<String>?
    var initialized: Boolean

    companion object{
        fun create() : SAnime {
            return SAnimeImpl()
        }
    }
}