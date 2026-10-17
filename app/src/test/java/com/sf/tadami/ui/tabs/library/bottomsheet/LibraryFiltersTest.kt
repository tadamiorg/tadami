package com.sf.tadami.ui.tabs.library.bottomsheet

import com.sf.tadami.domain.anime.LibraryAnime
import com.sf.tadami.preferences.library.LibraryFilter
import com.sf.tadami.source.model.SAnimeStatus
import com.sf.tadami.ui.components.data.LibraryItem
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryFiltersTest {

    private fun item(id: Long, unseenEpisodes: Long = 0L) = LibraryItem(
        LibraryAnime(
            id = id, source = 1L, url = "/$id", title = "Anime $id", rawTitle = null, language = null,
            thumbnailUrl = null, release = null, studio = null, author = null, status = SAnimeStatus.UNKNOWN,
            description = null, genres = null, favorite = true, initialized = true, episodes = 2L,
            lastUpdate = 0L, nextUpdate = 0L, fetchInterval = 0, unseenEpisodes = unseenEpisodes, episodeFlags = 0L,
            dateAdded = 0L,
        ),
        false,
    )

    private val library = listOf(item(1L), item(2L, unseenEpisodes = 1L), item(3L, unseenEpisodes = 2L), item(4L))

    private fun List<LibraryItem>.ids() = map { it.anime.id }

    @Test
    fun offTheWholeLibraryStays() {
        assertEquals(listOf(1L, 2L, 3L, 4L), library.downloadedOnly(null).ids())
    }

    @Test
    fun onlyTheAnimeWithADownloadStay() {
        assertEquals(listOf(2L, 4L), library.downloadedOnly(setOf(2L, 4L, 5L)).ids())
        assertEquals(emptyList<Long>(), library.downloadedOnly(emptySet()).ids())
    }

    @Test
    fun theUserFiltersStillApply() {
        val unseen = LibraryFilter(LibraryFilter.UNSEEN)

        assertEquals(listOf(2L), library.downloadedOnly(setOf(1L, 2L)).libraryFilters(unseen).ids())
    }
}
