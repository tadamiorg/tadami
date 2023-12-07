package com.sf.tadami.data.anime

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.sf.tadami.data.DataBaseHandler
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.LibraryAnime
import com.sf.tadami.domain.anime.UpdateAnime
import com.sf.tadami.network.api.model.AnimeFilterList
import com.sf.tadami.network.api.model.SAnime
import com.sf.tadami.network.database.listOfStringsAdapter
import com.sf.tadami.ui.tabs.animesources.AnimeSourcesManager
import kotlinx.coroutines.flow.Flow

interface AnimeRepository {

    suspend fun insertAnime(anime: Anime): Long?

    suspend fun insertNetworkToLocalAnime(anime: Anime): Anime

    fun getLibraryAnimesAsFlow(): Flow<List<LibraryAnime>>

    suspend fun getLibraryAnimes(): List<LibraryAnime>

    suspend fun getAnimeById(id: Long): Anime

    fun getAnimeByIdAsFlow(id: Long): Flow<Anime>

    suspend fun getAnimeBySourceAndUrl(source: String, url: String): Anime?

    fun getAnimeByUrlAndSourceIdAsFlow(sourceId: String, url: String): Flow<Anime?>

    suspend fun updateAnime(anime: UpdateAnime) : Boolean

    suspend fun updateAll(animes : List<UpdateAnime>)

    suspend fun deleteAnimesById(ids: List<Long>)

    fun getLatestPager(sourceId: String): Flow<PagingData<SAnime>>

    fun getSearchPager(
        sourceId: String,
        query: String,
        filters: AnimeFilterList
    ): Flow<PagingData<SAnime>>

}

class AnimeRepositoryImpl(
    private val handler: DataBaseHandler,
    private val sourceManager: AnimeSourcesManager,
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
                genres = anime.genres,
                favorite = anime.favorite,
                initiliazed = anime.initialized,
                lastUpdate = anime.lastUpdate,
                nextUpdate = anime.nextUpdate,
                calculateInterval = anime.fetchInterval.toLong()
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

    override fun getLibraryAnimesAsFlow(): Flow<List<LibraryAnime>> {
        return handler.subscribeToList { libraryQueries.getLibrary(AnimeMapper::mapLibraryAnime) }
    }

    override suspend fun getLibraryAnimes(): List<LibraryAnime> {
        return handler.awaitList {
            libraryQueries.getLibrary(AnimeMapper::mapLibraryAnime)
        }
    }

    override suspend fun getAnimeBySourceAndUrl(source: String, url: String): Anime? {
        return handler.awaitOneOrNull { animeQueries.getBySourceAndUrl(url, source, AnimeMapper::mapAnime) }
    }

    override fun getAnimeByUrlAndSourceIdAsFlow(sourceId: String, url: String): Flow<Anime?> {
        return handler.subscribeToOneOrNull {
            animeQueries.getBySourceAndUrl(
                url,
                sourceId,
                AnimeMapper::mapAnime
            )
        }
    }

    override suspend fun getAnimeById(id: Long): Anime {
        return handler.awaitOne { animeQueries.getById(id, AnimeMapper::mapAnime) }
    }

    override fun getAnimeByIdAsFlow(id: Long): Flow<Anime> {
        return handler.subscribeToOne { animeQueries.getById(id, AnimeMapper::mapAnime) }
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
                    animeId = anime.id,
                    calculateInterval = anime.fetchInterval?.toLong(),
                    nextUpdate = anime.nextUpdate,
                    lastUpdate = anime.lastUpdate
                )
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateAll(animes: List<UpdateAnime>) {
        handler.await {
            animes.forEach { anime ->
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
                    animeId = anime.id,
                    calculateInterval = anime.fetchInterval?.toLong(),
                    nextUpdate = anime.nextUpdate,
                    lastUpdate = anime.lastUpdate
                )
            }
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
                LatestPagingSource(source)
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
                SearchPagingSource(source, query, filters)
            }
        ).flow
    }
}