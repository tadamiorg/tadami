package com.sf.tadami.data.interactors.anime

import androidx.paging.PagingData
import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.data.episode.EpisodeRepository
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.LibraryAnime
import com.sf.tadami.domain.anime.UpdateAnime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.domain.episode.UpdateEpisode
import com.sf.tadami.source.Source
import com.sf.tadami.source.model.AnimeFilterList
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.model.SEpisode
import com.sf.tadami.utils.Lang
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateAnimeInteractorTest {

    private val anime = Anime.create().copy(id = 1L, favorite = true)

    private val dbEpisode = Episode.create().copy(
        id = 10L,
        animeId = anime.id,
        url = "/ep1",
        name = "Episode 1",
        episodeNumber = 1f,
        timeSeen = 500L,
        totalTime = 1000L,
        dateFetch = 42L,
        dateUpload = 100L,
        seen = true,
        sourceOrder = 0L,
    )

    @Test
    fun unchangedEpisodesAreNotWritten() = runBlocking {
        val episodes = FakeEpisodeRepository(listOf(dbEpisode))
        val animes = FakeAnimeRepository()

        val added = sync(episodes, animes, sEpisode("/ep1", "Episode 1", 1f, dateUpload = 100L))

        assertTrue(added.isEmpty())
        assertTrue(episodes.updated.isEmpty())
        assertTrue(animes.updates.none { it.lastUpdate != null })
    }

    @Test
    fun changedEpisodeUpdatesOnlyTheSourceFields() = runBlocking {
        val episodes = FakeEpisodeRepository(listOf(dbEpisode))
        val animes = FakeAnimeRepository()

        sync(episodes, animes, sEpisode("/ep1", "Renamed", 1f, dateUpload = 100L))

        val update = episodes.updated.single()
        assertEquals(dbEpisode.id, update.id)
        assertEquals("Renamed", update.name)
        // The seen state and progress are left to the database, they may have changed since the read
        assertEquals(null, update.seen)
        assertEquals(null, update.timeSeen)
        assertEquals(null, update.totalTime)
        assertEquals(null, update.dateFetch)
        assertTrue(animes.updates.any { it.lastUpdate != null })
    }

    @Test
    fun episodeMovedToAnotherUrlIsUpdatedInPlace() = runBlocking {
        val episodes = FakeEpisodeRepository(listOf(dbEpisode))
        val animes = FakeAnimeRepository()

        val added = sync(episodes, animes, sEpisode("/new-ep1", "Episode 1", 1f))

        // Same row: it keeps its seen state, progress and the history pointing to it
        assertTrue(added.isEmpty())
        assertTrue(episodes.added.isEmpty())
        assertTrue(episodes.deleted.isEmpty())
        val update = episodes.updated.single()
        assertEquals(dbEpisode.id, update.id)
        assertEquals("/new-ep1", update.url)
        assertEquals(null, update.seen)
        assertEquals(null, update.timeSeen)
    }

    @Test
    fun removedAndNewEpisodesWithOtherNumbersAreDeletedAndAdded() = runBlocking {
        val episodes = FakeEpisodeRepository(listOf(dbEpisode))
        val animes = FakeAnimeRepository()

        val added = sync(episodes, animes, sEpisode("/ep2", "Episode 2", 2f))

        assertEquals(listOf(dbEpisode.id), episodes.deleted)
        assertEquals("/ep2", added.single().url)
        assertTrue(episodes.updated.isEmpty())
    }

    private suspend fun sync(
        episodes: FakeEpisodeRepository,
        animes: FakeAnimeRepository,
        vararg remote: SEpisode,
    ): List<Episode> {
        val interactor = UpdateAnimeInteractor(animes, episodes, FetchIntervalInteractor(episodes))
        return interactor.awaitEpisodesSyncFromSource(anime, remote.toList(), FakeSource)
    }

    private fun sEpisode(url: String, name: String, number: Float, dateUpload: Long = 0L) =
        SEpisode.create().apply {
            this.url = url
            this.name = name
            this.episodeNumber = number
            this.dateUpload = dateUpload
        }

    private object FakeSource : Source {
        override val id = 1L
        override val name = "Fake"
        override val lang = Lang.UNKNOWN
    }

    private class FakeEpisodeRepository(var episodes: List<Episode>) : EpisodeRepository {
        val added = mutableListOf<Episode>()
        val deleted = mutableListOf<Long>()
        val updated = mutableListOf<UpdateEpisode>()

        override suspend fun sync(
            toAdd: List<Episode>,
            toUpdate: List<UpdateEpisode>,
            toDelete: List<Long>
        ): List<Episode> {
            added += toAdd
            updated += toUpdate
            deleted += toDelete
            return toAdd.mapIndexed { i, episode -> episode.copy(id = 100L + i) }
        }

        override suspend fun getEpisodesByAnimeId(animeId: Long) = episodes

        override suspend fun getEpisodeById(episodeId: Long): Episode = TODO()
        override fun getEpisodeByIdAsFlow(episodeId: Long): Flow<Episode> = TODO()
        override fun getEpisodesByAnimeIdAsFlow(animeId: Long): Flow<List<Episode>> = TODO()
        override suspend fun updateAll(episodes: List<UpdateEpisode>) = TODO()
        override suspend fun updateAllByAnimeIds(animeIdsWithEpUpdate: List<Pair<Long, UpdateEpisode>>) = TODO()
        override suspend fun update(episodeUpdate: UpdateEpisode) = TODO()
    }

    private class FakeAnimeRepository : AnimeRepository {
        val updates = mutableListOf<UpdateAnime>()

        override suspend fun updateAnime(anime: UpdateAnime): Boolean {
            updates += anime
            return true
        }

        override suspend fun insertAnime(anime: Anime): Long? = TODO()
        override suspend fun insertNetworkToLocalAnime(anime: Anime): Anime = TODO()
        override fun getLibraryAnimesAsFlow(): Flow<List<LibraryAnime>> = TODO()
        override suspend fun getLibraryAnimes(): List<LibraryAnime> = TODO()
        override suspend fun getAnimeById(id: Long): Anime = TODO()
        override fun getAnimeByIdAsFlow(id: Long): Flow<Anime> = TODO()
        override suspend fun getAnimeBySourceAndUrl(source: Long, url: String): Anime? = TODO()
        override fun getAnimeByUrlAndSourceIdAsFlow(sourceId: Long, url: String): Flow<Anime?> = TODO()
        override suspend fun updateAll(animes: List<UpdateAnime>) = TODO()
        override suspend fun deleteAnimesById(ids: List<Long>) = TODO()
        override fun getLatestPager(sourceId: Long): Flow<PagingData<SAnime>> = TODO()
        override fun getSearchPager(sourceId: Long, query: String, filters: AnimeFilterList): Flow<PagingData<SAnime>> = TODO()
    }
}
