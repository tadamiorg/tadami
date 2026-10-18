package com.sf.tadami.source.online

import com.sf.tadami.source.AnimesPage
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.model.SEpisode
import com.sf.tadami.source.model.StreamSource
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

abstract class ParsedAnimeHttpSource(sourceId : Long) : AnimeHttpSource() {
    override val id: Long = sourceId

    // Video Infos

    open fun getEpisodeTooltip() : String = throw Exception("Stub")

    // Search Animes

    protected abstract fun searchSelector(): String
    protected abstract fun searchAnimeFromElement(element: Element): SAnime?
    protected abstract fun searchAnimeNextPageSelector(): String?
    override fun searchAnimeParse(response: Response): AnimesPage = throw Exception("Stub!")

    // Latest Animes

    protected abstract fun latestSelector(): String
    protected abstract fun latestAnimeFromElement(element: Element): SAnime
    protected abstract fun latestAnimeNextPageSelector(): String?

    override fun latestAnimeParse(response: Response): AnimesPage = throw Exception("Stub!")

    // Anime Details

    override fun animeDetailsParse(response: Response): SAnime = throw Exception("Stub!")

    protected abstract fun animeDetailsParse(document: Document): SAnime

    // Episodes List

    @Deprecated("Use episodesListSelector instead", ReplaceWith("episodesListSelector()"))
    protected open fun episodesSelector(): String = throw Exception("Stub!")

    protected abstract fun episodesListSelector() : String

    protected abstract fun episodeFromElement(element: Element): SEpisode

    @Deprecated("Use episodesListParse instead", ReplaceWith("episodesListParse(response)"))
    override fun episodesParse(response: Response): List<SEpisode> = throw Exception("Stub!")

    override fun episodesListParse(response: Response): List<SEpisode> = throw Exception("Stub!")

    // Episode Sources

    @Deprecated("Use episodeSourcesSelector instead", ReplaceWith("episodeSourcesSelector()"))
    protected open fun streamSourcesSelector(): String = throw Exception("Stub!")

    protected abstract fun episodeSourcesSelector(): String

    @Deprecated("Use episodeSourcesFromElement instead", ReplaceWith("episodeSourcesFromElement()"))
    protected open fun streamSourcesFromElement(element: Element): List<StreamSource> = throw Exception("Stub!")

    protected abstract fun episodeSourcesFromElement(element: Element): List<StreamSource>

    override fun episodeSourcesParse(response: Response): List<StreamSource> = throw Exception("Stub!")
}