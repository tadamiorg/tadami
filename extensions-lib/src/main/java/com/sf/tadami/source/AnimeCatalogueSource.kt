package com.sf.tadami.source

import com.sf.tadami.source.model.AnimeFilterList
import com.sf.tadami.utils.Lang
import io.reactivex.rxjava3.core.Observable

/**
 * A [Source] the user browses: its latest anime, and a search with filters.
 *
 * @since extensions-lib 1.5
 */
interface AnimeCatalogueSource : Source {

    /**
     * The language of its anime.
     *
     * @since extensions-lib 1.5
     */
    override val lang: Lang

    /**
     * Whether it can search, so the app lists it in the global search.
     *
     * @since extensions-lib 1.5
     */
    val supportSearch : Boolean
        get() = true

    /**
     * Whether it has a list of the latest anime, see [getLatestAnime].
     *
     * @since extensions-lib 1.5
     */
    val supportRecent : Boolean
        get() = true

    /**
     * Whether the player can show a tooltip when an episode starts, see
     * [com.sf.tadami.source.online.ParsedAnimeHttpSource.getEpisodeTooltip] and
     * [com.sf.tadami.preferences.CommonKeys.SHOULD_SHOW_EPISODE_TOOLTIP].
     *
     * @since extensions-lib 1.5
     */
    val supportEpisodeTooltip : Boolean
        get() = false

    /**
     * The page [page] of the results of [query] with [filters], what [getSearchAnime] gives by
     * default.
     *
     * @param noToasts whether to show no toast, set by the global search
     * @since extensions-lib 1.5
     */
    @Deprecated("Override getSearchAnime instead: the app calls the suspend methods, which default to the Rx ones")
    fun fetchSearch(page: Int, query : String, filters: AnimeFilterList, noToasts : Boolean = false): Observable<AnimesPage>

    /**
     * The page [page] of the latest anime, what [getLatestAnime] gives by default.
     *
     * @since extensions-lib 1.5
     */
    @Deprecated("Override getLatestAnime instead: the app calls the suspend methods, which default to the Rx ones")
    fun fetchLatest(page: Int): Observable<AnimesPage>

    /**
     * The filters of the search, in their default state.
     *
     * @since extensions-lib 1.5
     */
    fun getFilterList(): AnimeFilterList

    /**
     * The page [page], from 1, of the results of [query] with [filters], what [fetchSearch] gives by
     * default.
     *
     * @param filters [getFilterList], with the states the user set
     * @param noToasts whether to show no toast, set by the global search
     * @since extensions-lib 1.7
     */
    suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList, noToasts: Boolean = false): AnimesPage =
        throw Exception("Stub!")

    /**
     * The page [page], from 1, of the latest anime, what [fetchLatest] gives by default.
     *
     * @since extensions-lib 1.7
     */
    suspend fun getLatestAnime(page: Int): AnimesPage = throw Exception("Stub!")
}
