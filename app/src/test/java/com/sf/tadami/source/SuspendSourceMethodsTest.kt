package com.sf.tadami.source

import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.DomainBackedSAnime
import com.sf.tadami.domain.anime.asLegacyAnime
import com.sf.tadami.domain.anime.toSAnime
import com.sf.tadami.source.model.AnimeFilterList
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.model.SAnimeImpl
import com.sf.tadami.source.model.SAnimeStatus
import com.sf.tadami.source.model.SEpisode
import com.sf.tadami.source.model.SEpisodeImpl
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.utils.Lang
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The suspend methods of the sources, which the app calls, and their defaults to the Rx fetches. */
class SuspendSourceMethodsTest {

    private val anime = Anime.create().copy(
        id = 42L,
        source = 7L,
        url = "/anime/42",
        title = "Title",
        favorite = true,
        episodeFlags = 64L,
        status = SAnimeStatus.ONGOING,
    )

    private fun sAnime(title: String) = SAnimeImpl().apply {
        this.title = title
        url = "/$title"
    }

    private fun sEpisode(name: String) = SEpisodeImpl().apply {
        this.name = name
        url = "/$name"
    }

    /** Built against lib 1.6 or earlier: only the Rx fetches. Records what it was called with. */
    private class RxSource : AnimeCatalogueSource {
        override val id = 7L
        override val name = "Rx"
        override val lang = Lang.ENGLISH

        var detailsAnime: Anime? = null
        var episodesAnime: Anime? = null
        var sourcesUrl: String? = null
        var search: List<Any>? = null

        override fun fetchSearch(page: Int, query: String, filters: AnimeFilterList, noToasts: Boolean): Observable<AnimesPage> {
            search = listOf(page, query, filters, noToasts)
            return Observable.just(AnimesPage(emptyList(), hasNextPage = true))
        }

        override fun fetchLatest(page: Int): Observable<AnimesPage> =
            Observable.just(AnimesPage(emptyList(), hasNextPage = page < 3))

        override fun getFilterList() = AnimeFilterList()

        override fun fetchAnimeDetails(anime: Anime): Observable<SAnime> {
            detailsAnime = anime
            return Observable.just(SAnimeImpl().apply { title = "Details of ${anime.title}"; url = anime.url })
        }

        override fun fetchEpisodesList(anime: Anime): Observable<List<SEpisode>> {
            episodesAnime = anime
            return Observable.just(listOf(SEpisodeImpl().apply { name = "1"; url = "${anime.url}/1" }))
        }

        override fun fetchEpisodeSources(url: String): Observable<List<StreamSource>> {
            sourcesUrl = url
            return Observable.just(listOf(StreamSource(url = "https://cdn$url")))
        }
    }

    /** Built against lib 1.7, with the suspend methods only. */
    private inner class SuspendSource : AnimeCatalogueSource {
        override val id = 8L
        override val name = "Suspend"
        override val lang = Lang.FRENCH

        override fun fetchSearch(page: Int, query: String, filters: AnimeFilterList, noToasts: Boolean): Observable<AnimesPage> =
            throw UnsupportedOperationException("Rx")

        override fun fetchLatest(page: Int): Observable<AnimesPage> = throw UnsupportedOperationException("Rx")

        override fun getFilterList() = AnimeFilterList()

        override suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList, noToasts: Boolean) =
            AnimesPage(listOf(sAnime("$query-$page")), hasNextPage = false)

        override suspend fun getLatestAnime(page: Int) = AnimesPage(listOf(sAnime("latest-$page")), hasNextPage = true)

        override suspend fun getAnimeDetails(anime: SAnime): SAnime = anime.apply { description = "Described" }

        override suspend fun getEpisodeList(anime: SAnime) = listOf(sEpisode("${anime.title} 1"))

        override suspend fun getEpisodeSources(episode: SEpisode) = listOf(StreamSource(url = "https://cdn${episode.url}"))
    }

    @Test
    fun sourceWithOnlyTheRxFetchesAnswersTheSuspendMethods() = runBlocking {
        val source = RxSource()
        val filters = AnimeFilterList()

        assertTrue(source.getSearchAnime(2, "query", filters, noToasts = true).hasNextPage)
        // AnimeFilterList equals nothing, not even itself
        assertEquals(listOf(2, "query", true), source.search?.filterIndexed { index, _ -> index != 2 })
        assertSame(filters, source.search?.get(2))
        source.getSearchAnime(1, "default", filters)
        assertEquals(false, source.search?.get(3))
        assertEquals(false, source.getLatestAnime(3).hasNextPage)

        assertEquals("Details of Title", source.getAnimeDetails(anime.toSAnime()).title)
        assertEquals(listOf("/anime/42/1"), source.getEpisodeList(anime.toSAnime()).map { it.url })
        assertEquals(listOf("https://cdn/ep/1"), source.getEpisodeSources(sEpisode("ep/1")).map { it.url })
        assertEquals("/ep/1", source.sourcesUrl)
    }

    @Test
    fun rxFetchesGetTheWholeAnimeOfTheApp() = runBlocking {
        val source = RxSource()
        source.getAnimeDetails(anime.toSAnime())
        source.getEpisodeList(anime.toSAnime())

        // The same instance, with what an SAnime doesn't have: its id, favorite, flags...
        assertSame(anime, source.detailsAnime)
        assertSame(anime, source.episodesAnime)
    }

    @Test
    fun rxFetchesGetAnAnimeOfTheSourceForAnotherSAnime() = runBlocking {
        val source = RxSource()
        source.getAnimeDetails(sAnime("other"))

        val legacy = source.detailsAnime!!
        assertEquals("other", legacy.title)
        assertEquals("/other", legacy.url)
        assertEquals(source.id, legacy.source)
    }

    @Test
    fun sourceWithOnlyTheSuspendMethodsAnswersThem() = runBlocking {
        val source = SuspendSource()

        assertEquals(listOf("q-2"), source.getSearchAnime(2, "q", AnimeFilterList(), noToasts = false).animes.map { it.title })
        assertEquals(listOf("latest-1"), source.getLatestAnime(1).animes.map { it.title })
        assertEquals("Described", source.getAnimeDetails(anime.toSAnime()).description)
        assertEquals(listOf("Title 1"), source.getEpisodeList(anime.toSAnime()).map { it.name })
        assertEquals(listOf("https://cdn/e"), source.getEpisodeSources(sEpisode("e")).map { it.url })
    }

    @Test
    fun stubSourceThrowsSourceNotInstalled() = runBlocking {
        val source = StubSource(id = 3L, name = "Missing", lang = Lang.FRENCH)
        val calls: List<suspend () -> Any> = listOf(
            { source.getSearchAnime(1, "", AnimeFilterList()) },
            { source.getLatestAnime(1) },
            { source.getAnimeDetails(anime.toSAnime()) },
            { source.getEpisodeList(anime.toSAnime()) },
            { source.getEpisodeSources(sEpisode("e")) },
        )
        calls.forEachIndexed { index, call ->
            try {
                call()
                fail("Call $index didn't throw")
            } catch (e: StubSource.SourceNotInstalledException) {
                assertEquals("Missing", e.sourceName)
            }
        }
    }

    @Test
    fun domainBackedSAnimeKeepsItsAnimeUpToDate() {
        val sAnime = anime.toSAnime()
        assertTrue(sAnime is DomainBackedSAnime)
        assertSame(anime, sAnime.asLegacyAnime(99L))
        assertEquals("Title", sAnime.title)
        assertEquals(SAnimeStatus.ONGOING, sAnime.status)

        sAnime.description = "New"
        sAnime.genres = listOf("Action")
        sAnime.initialized = true
        val legacy = sAnime.asLegacyAnime(99L)
        assertEquals(anime.copy(description = "New", genres = listOf("Action"), initialized = true), legacy)
        // Still the anime of the app, not one of the source passed
        assertEquals(42L, legacy.id)
        assertEquals(7L, legacy.source)
    }
}
