package com.sf.tadami.ui.tabs.library

import com.sf.tadami.domain.anime.LibraryAnime
import com.sf.tadami.domain.category.Category
import com.sf.tadami.source.model.SAnimeStatus
import com.sf.tadami.ui.components.data.LibraryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryTabsTest {

    private val action = Category(1L, "Action", 0L, 0L)
    private val drama = Category(2L, "Drama", 1L, 0L)

    private fun item(id: Long, vararg categories: Long) = LibraryItem(
        LibraryAnime(
            id = id, source = 1L, url = "/$id", title = "Anime $id", rawTitle = null, language = null,
            thumbnailUrl = null, release = null, studio = null, author = null, status = SAnimeStatus.UNKNOWN,
            description = null, genres = null, favorite = true, initialized = true, episodes = 0L,
            lastUpdate = 0L, nextUpdate = 0L, fetchInterval = 0, unseenEpisodes = 0L, episodeFlags = 0L,
            dateAdded = 0L,
            categories = categories.toList().ifEmpty { listOf(Category.UNCATEGORIZED_ID) },
        ),
        false,
    )

    private fun List<LibraryTab>.summary() = map { tab -> tab.category.id to tab.items.map { it.anime.id } }

    @Test
    fun withoutCategoriesTheDefaultTabHoldsTheWholeLibrary() {
        val tabs = libraryTabs(emptyList(), listOf(item(10L), item(11L)))

        assertEquals(listOf(0L to listOf(10L, 11L)), tabs.summary())
        assertFalse(tabs.showTabRow())
    }

    @Test
    fun anEmptyLibraryWithoutCategoriesStillHasTheDefaultTab() {
        assertEquals(listOf(0L to emptyList<Long>()), libraryTabs(emptyList(), emptyList()).summary())
    }

    @Test
    fun theDefaultTabComesFirstWhenSomeAnimeAreInNoCategory() {
        val tabs = libraryTabs(listOf(action, drama), listOf(item(10L, 1L), item(11L), item(12L, 2L)))

        assertEquals(listOf(0L to listOf(11L), 1L to listOf(10L), 2L to listOf(12L)), tabs.summary())
        assertTrue(tabs.showTabRow())
    }

    @Test
    fun theDefaultTabIsHiddenWhenEveryAnimeHasACategory() {
        val tabs = libraryTabs(listOf(action, drama), listOf(item(10L, 1L), item(12L, 2L)))

        assertEquals(listOf(1L to listOf(10L), 2L to listOf(12L)), tabs.summary())
    }

    @Test
    fun anEmptyCategoryStillHasItsTab() {
        val tabs = libraryTabs(listOf(action, drama), listOf(item(10L, 1L)))

        assertEquals(listOf(1L to listOf(10L), 2L to emptyList<Long>()), tabs.summary())
    }

    @Test
    fun aSingleVisibleCategoryHidesTheTabRow() {
        val tabs = libraryTabs(listOf(action), listOf(item(10L, 1L)))

        assertEquals(listOf(1L to listOf(10L)), tabs.summary())
        assertFalse(tabs.showTabRow())
    }

    @Test
    fun anAnimeInSeveralCategoriesIsInEachTab() {
        val tabs = libraryTabs(listOf(action, drama), listOf(item(10L, 1L, 2L)))

        assertEquals(listOf(1L to listOf(10L), 2L to listOf(10L)), tabs.summary())
    }

    @Test
    fun theCountShowsWithTheSettingOrWhileSearching() {
        val tab = LibraryTab(action, listOf(item(10L, 1L), item(11L, 1L)))

        assertNull(tab.badgeCount(showNumberOfItems = false, searching = false))
        assertEquals(2, tab.badgeCount(showNumberOfItems = true, searching = false))
        assertEquals(2, tab.badgeCount(showNumberOfItems = false, searching = true))
    }
}
