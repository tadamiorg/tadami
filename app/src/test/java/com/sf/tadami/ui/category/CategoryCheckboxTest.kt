package com.sf.tadami.ui.category

import androidx.compose.ui.state.ToggleableState
import com.sf.tadami.domain.category.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryCheckboxTest {

    private val action = Category(1L, "Action", 0L, 0L)
    private val drama = Category(2L, "Drama", 1L, 0L)
    private val later = Category(3L, "Later", 2L, 0L)
    private val categories = listOf(action, drama, later)

    @Test
    fun theRowsShowTheCategoriesOfOneAnime() {
        val rows = categoryCheckboxes(categories, listOf(listOf(2L)))

        assertEquals(
            listOf(ToggleableState.Off, ToggleableState.On, ToggleableState.Off),
            rows.map { it.state }
        )
    }

    @Test
    fun aCategoryOfOnlySomeAnimeIsIndeterminate() {
        // Both in Drama, only the first in Action
        val rows = categoryCheckboxes(categories, listOf(listOf(1L, 2L), listOf(2L)))

        assertEquals(
            listOf(ToggleableState.Indeterminate, ToggleableState.On, ToggleableState.Off),
            rows.map { it.state }
        )
        assertEquals(listOf(true, false, false), rows.map { it.mixed })
    }

    @Test
    fun theDefaultCategoryHasNoRow() {
        val rows = categoryCheckboxes(listOf(Category.default(), action), listOf(listOf(1L)))

        assertEquals(listOf(action), rows.map { it.category })
    }

    @Test
    fun aRowTogglesOnAndOff() {
        val row = CategoryCheckbox(action, ToggleableState.Off)

        assertEquals(ToggleableState.On, row.next().state)
        assertEquals(ToggleableState.Off, row.next().next().state)
    }

    @Test
    fun aMixedRowCanGoBackToIndeterminate() {
        val row = CategoryCheckbox(action, ToggleableState.Indeterminate, mixed = true)

        assertEquals(
            listOf(ToggleableState.Off, ToggleableState.On, ToggleableState.Indeterminate),
            generateSequence(row.next()) { it.next() }.take(3).map { it.state }.toList()
        )
    }

    @Test
    fun anIndeterminateRowChangesNothing() {
        val rows = listOf(
            CategoryCheckbox(action, ToggleableState.On),
            CategoryCheckbox(drama, ToggleableState.Indeterminate, mixed = true),
            CategoryCheckbox(later, ToggleableState.Off),
        )

        assertEquals(listOf(1L) to listOf(3L), rows.toChanges())
    }
}
