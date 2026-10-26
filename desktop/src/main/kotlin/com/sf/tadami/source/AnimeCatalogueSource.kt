package com.sf.tadami.source

import com.sf.tadami.source.model.AnimeFilterList
import com.sf.tadami.utils.Lang
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.rx3.awaitSingle

interface AnimeCatalogueSource : Source {

    override val lang: Lang

    val supportSearch : Boolean
        get() = true

    val supportRecent : Boolean
        get() = true

    val supportEpisodeTooltip : Boolean
        get() = false

    fun fetchSearch(page: Int, query : String, filters: AnimeFilterList, noToasts : Boolean = false): Observable<AnimesPage>

    fun fetchLatest(page: Int): Observable<AnimesPage>

    fun getFilterList(): AnimeFilterList

    /**
     * The page [page] of the results of [query] with [filters], what [fetchSearch] gives by default.
     *
     * @since extensions-lib 1.7
     */
    suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList, noToasts: Boolean = false): AnimesPage =
        fetchSearch(page, query, filters, noToasts).awaitSingle()

    /**
     * The page [page] of the latest anime, what [fetchLatest] gives by default.
     *
     * @since extensions-lib 1.7
     */
    suspend fun getLatestAnime(page: Int): AnimesPage = fetchLatest(page).awaitSingle()
}
