package com.sf.tadami.source.online

import com.sf.tadami.source.AnimesPage
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.model.SEpisode
import com.sf.tadami.source.model.StreamSource
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * An [AnimeHttpSource] parsing HTML pages with Jsoup: each list is the elements a CSS selector
 * matches, each one made into an item by a `...FromElement` method.
 *
 * @param sourceId the source ID, see [com.sf.tadami.source.Source.id]
 * @since extensions-lib 1.5
 */
abstract class ParsedAnimeHttpSource(sourceId : Long) : AnimeHttpSource() {
    /**
     * The source ID given to the constructor.
     *
     * @since extensions-lib 1.5
     */
    override val id: Long = sourceId

    // Video Infos

    /**
     * The tooltip the player shows when an episode starts, see
     * [com.sf.tadami.source.AnimeCatalogueSource.supportEpisodeTooltip].
     *
     * @since extensions-lib 1.5
     */
    open fun getEpisodeTooltip() : String = throw Exception("Stub")

    // Search Animes

    /**
     * Selects the results of a search page.
     *
     * @since extensions-lib 1.5
     */
    protected abstract fun searchSelector(): String

    /**
     * The anime of a result, null to skip it.
     *
     * @since extensions-lib 1.5
     */
    protected abstract fun searchAnimeFromElement(element: Element): SAnime?

    /**
     * Selects the link to the next search page, null when there's always one page.
     *
     * @since extensions-lib 1.5
     */
    protected abstract fun searchAnimeNextPageSelector(): String?

    /**
     * The anime of [searchSelector], and whether [searchAnimeNextPageSelector] matches.
     *
     * @since extensions-lib 1.5
     */
    override fun searchAnimeParse(response: Response): AnimesPage = throw Exception("Stub!")

    // Latest Animes

    /**
     * Selects the anime of a page of the latest ones.
     *
     * @since extensions-lib 1.5
     */
    protected abstract fun latestSelector(): String

    /**
     * The anime of an element of [latestSelector].
     *
     * @since extensions-lib 1.5
     */
    protected abstract fun latestAnimeFromElement(element: Element): SAnime

    /**
     * Selects the link to the next page of latest anime, null when there's always one page.
     *
     * @since extensions-lib 1.5
     */
    protected abstract fun latestAnimeNextPageSelector(): String?

    /**
     * The anime of [latestSelector], and whether [latestAnimeNextPageSelector] matches.
     *
     * @since extensions-lib 1.5
     */
    override fun latestAnimeParse(response: Response): AnimesPage = throw Exception("Stub!")

    // Anime Details

    /**
     * [animeDetailsParse] of the parsed page.
     *
     * @since extensions-lib 1.5
     */
    override fun animeDetailsParse(response: Response): SAnime = throw Exception("Stub!")

    /**
     * The details in the page [document].
     *
     * @since extensions-lib 1.5
     */
    protected abstract fun animeDetailsParse(document: Document): SAnime

    // Episodes List

    /**
     * What [episodesListSelector] gives by default.
     *
     * @since extensions-lib 1.5
     */
    @Deprecated("Use episodesListSelector instead", ReplaceWith("episodesListSelector()"))
    protected open fun episodesSelector(): String = throw Exception("Stub!")

    /**
     * Selects the episodes of a page.
     *
     * @since extensions-lib 1.5
     */
    protected abstract fun episodesListSelector() : String

    /**
     * The episode of an element of [episodesListSelector].
     *
     * @since extensions-lib 1.5
     */
    protected abstract fun episodeFromElement(element: Element): SEpisode

    /**
     * What [episodesListParse] gives by default.
     *
     * @since extensions-lib 1.5
     */
    @Deprecated("Use episodesListParse instead", ReplaceWith("episodesListParse(response)"))
    override fun episodesParse(response: Response): List<SEpisode> = throw Exception("Stub!")

    /**
     * The episodes of [episodesListSelector].
     *
     * @since extensions-lib 1.5
     */
    override fun episodesListParse(response: Response): List<SEpisode> = throw Exception("Stub!")

    // Episode Sources

    /**
     * What [episodeSourcesSelector] gives by default.
     *
     * @since extensions-lib 1.5
     */
    @Deprecated("Use episodeSourcesSelector instead", ReplaceWith("episodeSourcesSelector()"))
    protected open fun streamSourcesSelector(): String = throw Exception("Stub!")

    /**
     * Selects the players of an episode page.
     *
     * @since extensions-lib 1.5
     */
    protected abstract fun episodeSourcesSelector(): String

    /**
     * What [episodeSourcesFromElement] gives by default.
     *
     * @since extensions-lib 1.5
     */
    @Deprecated("Use episodeSourcesFromElement instead", ReplaceWith("episodeSourcesFromElement()"))
    protected open fun streamSourcesFromElement(element: Element): List<StreamSource> = throw Exception("Stub!")

    /**
     * The streams of an element of [episodeSourcesSelector].
     *
     * @since extensions-lib 1.5
     */
    protected abstract fun episodeSourcesFromElement(element: Element): List<StreamSource>

    /**
     * The streams of every element of [episodeSourcesSelector].
     *
     * @since extensions-lib 1.5
     */
    override fun episodeSourcesParse(response: Response): List<StreamSource> = throw Exception("Stub!")
}
