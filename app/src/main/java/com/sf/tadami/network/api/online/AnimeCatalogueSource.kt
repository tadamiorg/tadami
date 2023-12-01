package com.sf.tadami.network.api.online

import com.sf.tadami.network.api.model.AnimeFilterList
import com.sf.tadami.utils.Lang
import io.reactivex.rxjava3.core.Observable

interface AnimeCatalogueSource : Source {

    override val lang: Lang

    val supportSearch : Boolean
        get() = true

    val supportRecent : Boolean
        get() = true

    fun fetchSearch(page: Int, query : String, filters: AnimeFilterList, noToasts : Boolean = false): Observable<AnimesPage>

    fun fetchLatest(page: Int): Observable<AnimesPage>

    fun getFilterList(): AnimeFilterList
}