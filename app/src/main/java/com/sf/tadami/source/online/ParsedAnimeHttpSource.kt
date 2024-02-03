package com.sf.tadami.source

import com.sf.tadami.network.asJsoup
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.model.SEpisode
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.source.online.AnimesPage
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

abstract class ParsedAnimeHttpSource : AnimeHttpSource() {

    // Search
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

    // Latest
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

    // Details

    override fun animeDetailsParse(response: Response): SAnime {
        return animeDetailsParse(response.asJsoup())
    }

    protected abstract fun animeDetailsParse(document: Document): SAnime

    // Episodes

    protected abstract fun episodesSelector(): String
    protected abstract fun episodeFromElement(element: Element): SEpisode

    override fun episodesParse(response: Response): List<SEpisode> {
        val document = response.asJsoup()
        return document.select(episodesSelector()).map { episodeFromElement(it) }
    }

    // VideoList

    protected abstract fun streamSourcesSelector(): String
    protected abstract fun streamSourcesFromElement(element: Element): List<StreamSource>

    override fun episodeSourcesParse(response: Response): List<StreamSource> {
        val document = response.asJsoup()
        return document.select(streamSourcesSelector()).flatMap { streamSourcesFromElement(it) }
    }

}