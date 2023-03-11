package com.sf.animescraper.data.anime

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sf.animescraper.network.api.online.AnimeSource
import com.sf.animescraper.network.api.online.AnimesPage
import com.sf.animescraper.network.api.model.AnimeFilterList
import com.sf.animescraper.network.api.model.SAnime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.withContext

abstract class AnimePagingSource(
    protected val source: AnimeSource,
) : PagingSource<Long, SAnime>() {

    abstract suspend fun requestNextPage(currentPage: Int): AnimesPage

    override fun getRefreshKey(state: PagingState<Long, SAnime>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey ?: anchorPage?.nextKey
        }
    }

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, SAnime> {

        val page = params.key ?: 1

        val animePage = try {
            withContext(Dispatchers.IO){
                requestNextPage(page.toInt()).takeIf {
                    it.animes.isNotEmpty()
                } ?: throw Exception()
            }
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }

        return LoadResult.Page(
            data = animePage.animes,
            prevKey = null,
            nextKey = if (animePage.hasNextPage) page + 1 else null,
        )

    }
}

class LatestPagingSource(source: AnimeSource) : AnimePagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): AnimesPage {
        return try {
            source.fetchLatest(currentPage).singleOrError().await()
        }catch(e : Exception) {
            Log.d("LatestPagingError",e.toString(),e)
            AnimesPage(
                emptyList(),
                false
            )
        }

    }
}

class SearchPagingSource(source: AnimeSource, private val query: String, private val filters: AnimeFilterList) :
    AnimePagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): AnimesPage {
        return try {
            source.fetchSearch(currentPage, query, filters).singleOrError().await()
        }catch(e : Exception) {
            Log.e("SearchPagingError",e.stackTrace.toString())
            AnimesPage(
                emptyList(),
                false
            )
        }
    }
}