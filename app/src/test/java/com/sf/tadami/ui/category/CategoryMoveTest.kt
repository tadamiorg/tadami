package com.sf.tadami.ui.category

import com.sf.tadami.data.interactors.category.FakeCategoryRepository
import com.sf.tadami.data.interactors.category.ReorderCategory
import com.sf.tadami.domain.category.Category
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryMoveTest {

    private val categories = listOf(
        Category(1L, "Action", 0L, 0L),
        Category(2L, "Drama", 1L, 0L),
        Category(3L, "Later", 2L, 0L),
    )

    @Test
    fun aCategoryMovesOneStep() {
        assertEquals(0, categories.moveIndex(2L, up = true))
        assertEquals(2, categories.moveIndex(2L, up = false))
    }

    @Test
    fun theEndsDontMoveFurther() {
        assertNull(categories.moveIndex(1L, up = true))
        assertNull(categories.moveIndex(3L, up = false))
        assertNull(categories.moveIndex(4L, up = true))
    }

    @Test
    fun theOrderFollowsTheMoves() = runBlocking {
        val repository = FakeCategoryRepository(categories)
        val reorder = ReorderCategory(repository)
        suspend fun move(id: Long, up: Boolean) {
            reorder.await(id, repository.getAll().moveIndex(id, up)!!)
        }

        move(3L, up = true)
        assertEquals(listOf("Action", "Later", "Drama"), repository.getAll().map { it.name })

        move(1L, up = false)
        assertEquals(listOf("Later", "Action", "Drama"), repository.getAll().map { it.name })
        assertEquals(listOf(0L, 1L, 2L), repository.getAll().map { it.order })
    }
}
