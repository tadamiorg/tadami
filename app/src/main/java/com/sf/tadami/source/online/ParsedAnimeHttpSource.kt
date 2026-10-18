package com.sf.tadami.source.online

import com.sf.tadami.network.asJsoup
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

    open fun getEpisodeTooltip() : String = throw IllegalStateException("Not used")

    // Search Animes

    protected abstract fun searchSelector(): String
    protected abstract fun searchAnimeFromElement(element: Element): SAnime?
    protected abstract fun searchAnimeNextPageSelector(): String?
    override fun searchAnimeParse(response: Response): AnimesPage {
        val document = response.asJsoup()

        val animes = document.select(searchSelector()).mapNotNull { element ->
            searchAnimeFromElement(element)
        }

        val hasNextPage = searchAnimeNextPageSelector()?.let { selector ->
            document.select(selector).first()
        } != null

        return AnimesPage(animes, hasNextPage)
    }

    // Latest Animes

    protected abstract fun latestSelector(): String
    protected abstract fun latestAnimeFromElement(element: Element): SAnime
    protected abstract fun latestAnimeNextPageSelector(): String?

    override fun latestAnimeParse(response: Response): AnimesPage {
        val document = response.asJsoup()

        val animes = document.select(latestSelector()).map { element ->
            latestAnimeFromElement(element)
        }

        val hasNextPage = latestAnimeNextPageSelector()?.let { selector ->
            document.select(selector).first()
        } != null

        return AnimesPage(animes, hasNextPage)
    }

    // Anime Details

    override fun animeDetailsParse(response: Response): SAnime {
        return animeDetailsParse(response.asJsoup())
    }

    protected abstract fun animeDetailsParse(document: Document): SAnime

    // Episodes List

    // The deprecated hooks below are open with a throwing default rather than abstract: extensions
    // built against extensions-lib may not implement them, and only override their replacement
    @Deprecated("Use episodesListSelector instead", ReplaceWith("episodesListSelector()"))
    protected open fun episodesSelector(): String =
        throw IllegalStateException("Not used: override episodesListSelector")

    @Suppress("DEPRECATION")
    protected open fun episodesListSelector() : String {
        return episodesSelector()
    }

    protected abstract fun episodeFromElement(element: Element): SEpisode

    @Deprecated("Use episodesListParse instead", ReplaceWith("episodesListParse(response)"))
    override fun episodesParse(response: Response): List<SEpisode> {
        val document = response.asJsoup()
        return document.select(episodesListSelector()).map { episodeFromElement(it) }
    }

    @Suppress("DEPRECATION")
    override fun episodesListParse(response: Response): List<SEpisode> {
        return episodesParse(response)
    }

    // Episodes Sources

    @Deprecated("Use episodeSourcesSelector instead", ReplaceWith("episodeSourcesSelector()"))
    protected open fun streamSourcesSelector(): String =
        throw IllegalStateException("Not used: override episodeSourcesSelector")

    @Suppress("DEPRECATION")
    protected open fun episodeSourcesSelector(): String {
        return streamSourcesSelector()
    }

    @Deprecated("Use episodeSourcesFromElement instead", ReplaceWith("episodeSourcesFromElement()"))
    protected open fun streamSourcesFromElement(element: Element): List<StreamSource> =
        throw IllegalStateException("Not used: override episodeSourcesFromElement")

    @Suppress("DEPRECATION")
    protected open fun episodeSourcesFromElement(element: Element): List<StreamSource> {
        return streamSourcesFromElement(element)
    }

    override fun episodeSourcesParse(response: Response): List<StreamSource> {
        val document = response.asJsoup()
        return document.select(episodeSourcesSelector()).flatMap { episodeSourcesFromElement(it) }
    }

}