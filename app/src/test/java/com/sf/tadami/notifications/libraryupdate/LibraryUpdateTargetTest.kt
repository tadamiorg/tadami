package com.sf.tadami.notifications.libraryupdate

import com.sf.tadami.domain.anime.LibraryAnime
import com.sf.tadami.domain.category.Category
import com.sf.tadami.source.model.SAnimeStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryUpdateTargetTest {

    @Test
    fun anUpdateWithoutTargetFetchesTheEpisodes() {
        assertEquals(LibraryUpdateTarget.EPISODES, LibraryUpdateTarget.of(null))
        assertEquals(LibraryUpdateTarget.EPISODES, LibraryUpdateTarget.of("unknown"))
        assertEquals(LibraryUpdateTarget.DETAILS, LibraryUpdateTarget.of(LibraryUpdateTarget.DETAILS.name))
    }

    @Test
    fun theEpisodesUpdateDoesntFetchTheDetails() {
        assertEquals(
            LibraryUpdateParts(episodes = true, details = false, refreshCovers = false),
            LibraryUpdateTarget.EPISODES.parts(),
        )
    }

    @Test
    fun theDetailsRefreshFetchesTheDetailsAndCoversOnly() {
        assertEquals(
            LibraryUpdateParts(episodes = false, details = true, refreshCovers = true),
            LibraryUpdateTarget.DETAILS.parts(),
        )
    }

    private fun anime(id: Long, vararg categories: Long) = LibraryAnime(
        id = id, source = 1L, url = "/$id", title = "Anime $id", rawTitle = null, language = null,
        thumbnailUrl = null, release = null, studio = null, author = null, status = SAnimeStatus.UNKNOWN,
        description = null, genres = null, favorite = true, initialized = true, episodes = 0L,
        lastUpdate = 0L, nextUpdate = 0L, fetchInterval = 0, unseenEpisodes = 10L, episodeFlags = 0L,
        dateAdded = 0L,
        categories = categories.toList().ifEmpty { listOf(Category.UNCATEGORIZED_ID) },
    )

    // 10 in no category, 11 in Action, 12 in Drama
    private val library = listOf(anime(10L), anime(11L, 1L), anime(12L, 2L))

    @Test
    fun theRefreshGoesThroughTheWholeLibraryOrTheCategoryShown() {
        assertEquals(listOf(10L, 11L, 12L), library.toRefresh(null).map { it.id })
        assertEquals(listOf(11L), library.toRefresh(1L).map { it.id })
    }
}
