package com.sf.tadami.source.model

class SAnimeImpl : SAnime {
    override lateinit var title: String
    override lateinit var url: String
    override var rawTitle: String? = null
    override var thumbnailUrl: String? = null
    override var release: String? = null
    override var studio: String? = null
    override var author: String? = null
    override var status: SAnimeStatus = SAnimeStatus.UNKNOWN
    override var description : String? = null
    override var genres: List<String>? = null
    override var initialized: Boolean = false
    override fun toString(): String {
        return "SAnimeImpl(title='$title', rawTitle=$rawTitle, url='$url', thumbnailUrl=$thumbnailUrl, " +
                "release=$release, studio=$studio, author=$author, status=$status, description=$description, " +
                "genres=$genres, initialized=$initialized)"
    }
}
