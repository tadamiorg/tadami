package com.sf.tadami.ui.tabs.library

import com.sf.tadami.domain.anime.LibraryAnime
import com.sf.tadami.source.model.SAnimeStatus
import com.sf.tadami.ui.components.data.LibraryItem
import org.junit.Assert.assertEquals
import org.junit.Test

class LibrarySelectionTest {

    private fun item(id: Long, selected: Boolean = false) = LibraryItem(
        LibraryAnime(
            id = id, source = 1L, url = "/$id", title = "Anime $id", rawTitle = null, language = null,
            thumbnailUrl = null, release = null, studio = null, author = null, status = SAnimeStatus.UNKNOWN,
            description = null, genres = null, favorite = true, initialized = true, episodes = 0L,
            lastUpdate = 0L, nextUpdate = 0L, fetchInterval = 0, unseenEpisodes = 0L, episodeFlags = 0L,
            dateAdded = 0L,
        ),
        selected,
    )

    private fun List<LibraryItem>.selectedIds() = filter { it.selected }.map { it.anime.id }

    @Test
    fun selectAllOnlySelectsTheDisplayedItems() {
        val library = (1L..5L).map { item(it) }

        val result = library.selectDisplayed(setOf(2L, 4L)) { true }

        assertEquals(listOf(2L, 4L), result.selectedIds())
    }

    @Test
    fun invertOnlyInvertsTheDisplayedItems() {
        val library = listOf(item(1L), item(2L, selected = true), item(3L), item(4L))

        val result = library.selectDisplayed(setOf(1L, 2L, 3L)) { !it.selected }

        assertEquals(listOf(1L, 3L), result.selectedIds())
    }

    @Test
    fun selectAllOnATabKeepsTheSelectionOfTheOtherTabs() {
        val library = listOf(item(1L, selected = true), item(2L), item(3L), item(4L, selected = true))

        // 1 and 2 shown in another tab, 3 on the tab shown, 4 hidden by the filters
        val result = library.selectDisplayed(setOf(1L, 2L, 3L), pageIds = setOf(3L)) { true }

        assertEquals(listOf(1L, 3L), result.selectedIds())
    }

    @Test
    fun invertOnATabOnlyInvertsItsItems() {
        val library = listOf(item(1L, selected = true), item(2L, selected = true), item(3L))

        val result = library.selectDisplayed(setOf(1L, 2L, 3L), pageIds = setOf(2L, 3L)) { !it.selected }

        assertEquals(listOf(1L, 3L), result.selectedIds())
    }

    @Test
    fun aHiddenItemDoesNotStaySelected() {
        val library = listOf(item(1L, selected = true), item(2L, selected = true), item(3L))

        val result = library.selectDisplayed(setOf(2L, 3L)) { it.selected }

        assertEquals(listOf(2L), result.selectedIds())
    }
}
