package com.sf.tadami.data.anime

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sf.tadami.source.model.AnimeFilterList
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.AnimeCatalogueSource
import com.sf.tadami.source.AnimesPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.withContext

abstract class AnimePagingSource(
    protected val source: AnimeCatalogueSource,
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
                } ?: throw NoResultException()
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

class LatestPagingSource(source: AnimeCatalogueSource) : AnimePagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): AnimesPage {
        return source.fetchLatest(currentPage).singleOrError().await()
    }
}

class SearchPagingSource(source: AnimeCatalogueSource, private val query: String, private val filters: AnimeFilterList) :
    AnimePagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): AnimesPage {
        return source.fetchSearch(currentPage, query, filters).singleOrError().await()
    }
}

class NoResultException : Exception()