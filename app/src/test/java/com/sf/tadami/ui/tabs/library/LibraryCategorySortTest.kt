package com.sf.tadami.ui.tabs.library

import com.sf.tadami.domain.category.Category
import com.sf.tadami.preferences.library.LibrarySort
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryCategorySortTest {

    private val librarySort = LibrarySort(LibrarySort.DEFAULT_SORT)
    // Unseen count, descending
    private val unseenSort = LibrarySort(LibrarySort.UNSEEN_COUNT)
    // Episode count, ascending
    private val episodeSort = LibrarySort(LibrarySort.EPISODE_COUNT or LibrarySort.SORT_DIRECTION)

    private val action = Category(1L, "Action", 0L, unseenSort.flags)
    private val drama = Category(2L, "Drama", 1L, 0L)

    @Test
    fun withoutThePerCategorySortEveryTabUsesTheLibrarySort() {
        listOf(action, drama, Category.default()).forEach {
            assertEquals(librarySort, categorySort(it, perCategory = false, librarySort, episodeSort.flags))
        }
    }

    @Test
    fun withThePerCategorySortATabUsesItsOwn() {
        assertEquals(unseenSort, categorySort(action, perCategory = true, librarySort, episodeSort.flags))
        // The Default category's sort is in a setting
        assertEquals(episodeSort, categorySort(Category.default(), perCategory = true, librarySort, episodeSort.flags))
    }

    @Test
    fun aCategoryWithoutASortYetUsesTheLibrarySort() {
        assertEquals(librarySort, categorySort(drama, perCategory = true, librarySort, episodeSort.flags))
        assertEquals(librarySort, categorySort(Category.default(), perCategory = true, librarySort, 0L))
    }

    @Test
    fun selectingTheCurrentTypeSwapsTheDirection() {
        val descending = librarySort.select(LibrarySort.ALPHABETICALLY)

        assertEquals(LibrarySort.SortType.Alphabetical, descending.sortType)
        assertEquals(false, descending.isAscending)
        assertEquals(librarySort, descending.select(LibrarySort.ALPHABETICALLY))
    }

    @Test
    fun selectingAnotherTypeKeepsTheDirection() {
        val sort = librarySort.select(LibrarySort.EPISODE_COUNT)

        assertEquals(episodeSort, sort)
        // Never 0: a chosen sort can't be taken for none
        assertEquals(LibrarySort.UNSEEN_COUNT, LibrarySort(LibrarySort.EPISODE_COUNT).select(LibrarySort.UNSEEN_COUNT).flags)
    }
}
