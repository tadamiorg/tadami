package com.sf.tadami.ui.tabs.library.bottomsheet

import com.sf.tadami.domain.anime.LibraryAnime
import com.sf.tadami.preferences.library.LibrarySort
import com.sf.tadami.source.model.SAnimeStatus
import com.sf.tadami.ui.components.data.LibraryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SortComparatorTest {

    private fun item(title: String) = LibraryItem(
        LibraryAnime(
            id = 0L, source = 1L, url = "/$title", title = title, rawTitle = null, language = null,
            thumbnailUrl = null, release = null, studio = null, author = null, status = SAnimeStatus.UNKNOWN,
            description = null, genres = null, favorite = true, initialized = true, episodes = 0L,
            lastUpdate = 0L, nextUpdate = 0L, fetchInterval = 0, unseenEpisodes = 0L, episodeFlags = 0L,
            dateAdded = 0L,
        ),
        false,
    )

    private val library = listOf(item("bleach"), item("Naruto"), item("Akira"), item("one piece"))

    private fun List<LibraryItem>.sortedBy(sort: LibrarySort) =
        sortedWith { a1, a2 -> sortComparator(sort).invoke(a1, a2) }.map { it.anime.title }

    @Test
    fun theDefaultSortIsAlphabeticalAscending() {
        val sort = LibrarySort(LibrarySort.DEFAULT_SORT)
        assertTrue(sort.sortType is LibrarySort.SortType.Alphabetical)
        assertTrue(sort.isAscending)
    }

    @Test
    fun alphabeticalSortIgnoresTheCase() {
        assertEquals(
            listOf("Akira", "bleach", "Naruto", "one piece"),
            library.sortedBy(LibrarySort(LibrarySort.DEFAULT_SORT)),
        )
        assertEquals(
            listOf("one piece", "Naruto", "bleach", "Akira"),
            library.sortedBy(LibrarySort(LibrarySort.ALPHABETICALLY)),
        )
    }
}
