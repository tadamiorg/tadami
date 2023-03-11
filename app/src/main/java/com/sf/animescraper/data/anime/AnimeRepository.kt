package com.sf.animescraper.data.anime

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.sf.animescraper.data.DataBaseHandler
import com.sf.animescraper.domain.anime.Anime
import com.sf.animescraper.domain.anime.UpdateAnime
import com.sf.animescraper.network.api.model.AnimeFilterList
import com.sf.animescraper.network.api.model.SAnime
import com.sf.animescraper.network.database.listOfStringsAdapter
import com.sf.animescraper.ui.tabs.animesources.AnimeSourcesManager
import kotlinx.coroutines.flow.Flow
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

interface AnimeRepository {

    suspend fun insertAnime(anime: Anime): Long?

    suspend fun insertNetworkToLocalAnime(anime: Anime): Anime

    fun getFavoriteAnimes(): Flow<List<Anime>>

    suspend fun getAnimeById(id: Long): Anime

    fun getAnimeByIdAsFlow(id: Long): Flow<Anime>

    suspend fun getAnimeBySourceAndUrl(source: String, url: String): Anime?

    fun getMangaByUrlAndSourceIdAsFlow(sourceId: String, url: String): Flow<Anime?>

    suspend fun updateAnime(anime: UpdateAnime) : Boolean

    suspend fun deleteAnimesById(ids: List<Long>)

    fun getLatestPager(sourceId: String): Flow<PagingData<SAnime>>

    fun getSearchPager(
        sourceId: String,
        query: String,
        filters: AnimeFilterList
    ): Flow<PagingData<SAnime>>

}

class AnimeRepositoryImpl(
    private val handler: DataBaseHandler = Injekt.get(),
    private val sourceManager: AnimeSourcesManager = Injekt.get(),
) : AnimeRepository {

    override suspend fun insertAnime(anime: Anime): Long? {

        return handler.awaitOneOrNull {
            animeQueries.insert(
                source = anime.source,
                url = anime.url,
                title = anime.title,
                thumbnailUrl = anime.thumbnailUrl,
                release = anime.release,
                status = anime.status,
                description = anime.description,
                genre = anime.genres,
                favorite = anime.favorite,
                initiliazed = anime.initialized
            )
            animeQueries.selectLastInsertedRowId()
        }
    }

    override suspend fun insertNetworkToLocalAnime(anime: Anime): Anime {
        val dbAnime = getAnimeBySourceAndUrl(anime.source, anime.url)
        return when {
            dbAnime == null -> {
                val id = insertAnime(anime)
                anime.copy(id = id!!)
            }
            !dbAnime.favorite -> {
                dbAnime.copy(title = anime.title)
            }
            else -> {
                dbAnime
            }
        }
    }

    override fun getFavoriteAnimes(): Flow<List<Anime>> {
        return handler.subscribeToList { animeQueries.getFavorites(animeMapper) }
    }

    override suspend fun getAnimeBySourceAndUrl(source: String, url: String): Anime? {
        return handler.awaitOneOrNull { animeQueries.getBySourceAndUrl(url, source, animeMapper) }
    }

    override fun getMangaByUrlAndSourceIdAsFlow(sourceId: String, url: String): Flow<Anime?> {
        return handler.subscribeToOneOrNull {
            animeQueries.getBySourceAndUrl(
                url,
                sourceId,
                animeMapper
            )
        }
    }

    override suspend fun getAnimeById(id: Long): Anime {
        return handler.awaitOne { animeQueries.getById(id, animeMapper) }
    }

    override fun getAnimeByIdAsFlow(id: Long): Flow<Anime> {
        return handler.subscribeToOne { animeQueries.getById(id, animeMapper) }
    }

    override suspend fun updateAnime(anime: UpdateAnime) : Boolean {
        return try {
            handler.await {
                animeQueries.update(
                    source = anime.source,
                    url = anime.url,
                    title = anime.title,
                    thumbnailUrl = anime.thumbnailUrl,
                    release = anime.release,
                    status = anime.status,
                    description = anime.description,
                    genres = anime.genres?.let(listOfStringsAdapter::encode),
                    favorite = anime.favorite,
                    initialized = anime.initialized,
                    animeId = anime.id
                )
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteAnimesById(ids: List<Long>) {
        return handler.await { animeQueries.delete(ids) }
    }

    override fun getLatestPager(sourceId: String): Flow<PagingData<SAnime>> {
        val source = sourceManager.getExtensionById(sourceId)
        return Pager(
            config = PagingConfig(
                pageSize = 20
            ),
            pagingSourceFactory = {
                LatestPagingSource(source!!)
            }
        ).flow
    }

    override fun getSearchPager(
        sourceId: String,
        query: String,
        filters: AnimeFilterList
    ): Flow<PagingData<SAnime>> {
        val source = sourceManager.getExtensionById(sourceId)
        return Pager(
            config = PagingConfig(
                pageSize = 20
            ),
            pagingSourceFactory = {
                SearchPagingSource(source!!, query, filters)
            }
        ).flow
    }
}