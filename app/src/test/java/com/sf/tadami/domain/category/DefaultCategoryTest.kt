package com.sf.tadami.domain.category

import com.sf.tadami.preferences.library.LibraryPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultCategoryTest {

    private val categories = listOf(Category(1L, "Action", 0L, 0L), Category(2L, "Drama", 1L, 0L))

    @Test
    fun theUserIsAskedByDefault() {
        assertEquals(
            DefaultCategoryChoice.Ask,
            defaultCategoryChoice(LibraryPreferences.DEFAULT_CATEGORY_ASK, categories)
        )
    }

    @Test
    fun theChosenCategoryIsUsed() {
        assertEquals(DefaultCategoryChoice.Categories(listOf(2L)), defaultCategoryChoice(2, categories))
    }

    @Test
    fun theDefaultCategoryCanBeChosen() {
        assertEquals(
            DefaultCategoryChoice.Categories(emptyList()),
            defaultCategoryChoice(Category.UNCATEGORIZED_ID.toInt(), categories)
        )
    }

    @Test
    fun withoutCategoriesTheAnimeGoesToTheDefaultOneWithoutAsking() {
        assertEquals(
            DefaultCategoryChoice.Categories(emptyList()),
            defaultCategoryChoice(LibraryPreferences.DEFAULT_CATEGORY_ASK, emptyList())
        )
        // The Default category doesn't count as one to choose from
        assertEquals(
            DefaultCategoryChoice.Categories(emptyList()),
            defaultCategoryChoice(LibraryPreferences.DEFAULT_CATEGORY_ASK, listOf(Category.default()))
        )
    }

    @Test
    fun aDeletedCategoryIsAskedFor() {
        assertEquals(DefaultCategoryChoice.Ask, defaultCategoryChoice(42, categories))
    }
}
