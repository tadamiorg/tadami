package com.sf.tadami.data.anime

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.sf.tadami.data.DataBaseHandler
import com.sf.tadami.data.listOfStringsAdapter
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.LibraryAnime
import com.sf.tadami.domain.anime.UpdateAnime
import com.sf.tadami.source.AnimeCatalogueSource
import com.sf.tadami.source.model.AnimeFilterList
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.ui.tabs.browse.SourceManager
import kotlinx.coroutines.flow.Flow

interface AnimeRepository {

    suspend fun insertAnime(anime: Anime): Long?

    suspend fun insertNetworkToLocalAnime(anime: Anime): Anime

    fun getLibraryAnimesAsFlow(): Flow<List<LibraryAnime>>

    suspend fun getLibraryAnimes(): List<LibraryAnime>

    suspend fun getAnimeById(id: Long): Anime

    fun getAnimeByIdAsFlow(id: Long): Flow<Anime>

    suspend fun getAnimeBySourceAndUrl(source: Long, url: String): Anime?

    fun getAnimeByUrlAndSourceIdAsFlow(sourceId: Long, url: String): Flow<Anime?>

    suspend fun updateAnime(anime: UpdateAnime) : Boolean

    suspend fun updateAll(animes : List<UpdateAnime>)

    suspend fun deleteAnimesById(ids: List<Long>)

    fun getLatestPager(sourceId: Long): Flow<PagingData<SAnime>>

    fun getSearchPager(
        sourceId: Long,
        query: String,
        filters: AnimeFilterList
    ): Flow<PagingData<SAnime>>

}

class AnimeRepositoryImpl(
    private val handler: DataBaseHandler,
    private val sourceManager: SourceManager,
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
                calculateInterval = anime.fetchInterval.toLong(),
                episodeFlags = anime.episodeFlags,
                dateAdded = anime.dateAdded
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
        return handler.subscribeToList { libraryViewQueries.getLibrary(AnimeMapper::mapLibraryAnime) }
    }

    override suspend fun getLibraryAnimes(): List<LibraryAnime> {
        return handler.awaitList {
            libraryViewQueries.getLibrary(AnimeMapper::mapLibraryAnime)
        }
    }

    override suspend fun getAnimeBySourceAndUrl(source: Long, url: String): Anime? {
        return handler.awaitOneOrNull { animeQueries.getBySourceAndUrl(url, source, AnimeMapper::mapAnime) }
    }

    override fun getAnimeByUrlAndSourceIdAsFlow(sourceId: Long, url: String): Flow<Anime?> {
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
                    lastUpdate = anime.lastUpdate,
                    episodeFlags = anime.episodeFlags,
                    dateAdded = anime.dateAdded
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
                    lastUpdate = anime.lastUpdate,
                    episodeFlags = anime.episodeFlags,
                    dateAdded = anime.dateAdded
                )
            }
        }
    }

    override suspend fun deleteAnimesById(ids: List<Long>) {
        return handler.await { animeQueries.delete(ids) }
    }

    override fun getLatestPager(sourceId: Long): Flow<PagingData<SAnime>> {
        val source = sourceManager.get(sourceId) as AnimeCatalogueSource
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
        sourceId: Long,
        query: String,
        filters: AnimeFilterList
    ): Flow<PagingData<SAnime>> {
        val source = sourceManager.get(sourceId) as AnimeCatalogueSource
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