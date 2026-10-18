package com.sf.tadami.notifications.libraryupdate

import com.sf.tadami.domain.anime.LibraryAnime
import com.sf.tadami.domain.category.Category
import com.sf.tadami.source.model.SAnimeStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryUpdateCategoriesTest {

    private fun anime(id: Long, vararg categories: Long) = LibraryAnime(
        id = id, source = 1L, url = "/$id", title = "Anime $id", rawTitle = null, language = null,
        thumbnailUrl = null, release = null, studio = null, author = null, status = SAnimeStatus.UNKNOWN,
        description = null, genres = null, favorite = true, initialized = true, episodes = 0L,
        lastUpdate = 0L, nextUpdate = 0L, fetchInterval = 0, unseenEpisodes = 0L, episodeFlags = 0L,
        dateAdded = 0L,
        categories = categories.toList().ifEmpty { listOf(Category.UNCATEGORIZED_ID) },
    )

    // 10 in no category, 11 in Action, 12 in Drama, 13 in both
    private val library = listOf(anime(10L), anime(11L, 1L), anime(12L, 2L), anime(13L, 1L, 2L))

    private fun List<LibraryAnime>.ids() = map { it.id }

    @Test
    fun noCategoryChosenUpdatesTheWholeLibrary() {
        assertEquals(listOf(10L, 11L, 12L, 13L), library.inUpdateCategories(emptySet(), emptySet()).ids())
    }

    @Test
    fun onlyTheIncludedCategoriesAreUpdated() {
        assertEquals(listOf(11L, 13L), library.inUpdateCategories(setOf("1"), emptySet()).ids())
    }

    @Test
    fun theDefaultCategoryCanBeIncluded() {
        assertEquals(listOf(10L, 12L, 13L), library.inUpdateCategories(setOf("0", "2"), emptySet()).ids())
    }

    @Test
    fun anExcludedCategoryWinsOverAnIncludedOne() {
        assertEquals(listOf(11L), library.inUpdateCategories(setOf("1"), setOf("2")).ids())
    }

    @Test
    fun excludingAloneKeepsTheOtherCategories() {
        assertEquals(listOf(11L, 12L, 13L), library.inUpdateCategories(emptySet(), setOf("0")).ids())
    }
}
