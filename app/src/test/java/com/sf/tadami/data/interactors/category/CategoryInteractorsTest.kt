package com.sf.tadami.data.interactors.category

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.sf.tadami.domain.category.Category
import com.sf.tadami.preferences.library.LibraryPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryInteractorsTest {

    private class InMemoryDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        private val mutex = Mutex()
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            mutex.withLock { transform(state.value).also { state.value = it } }
    }

    private fun category(id: Long, order: Long, name: String) = Category(id, name, order, 0L)

    private fun repository() = FakeCategoryRepository(
        listOf(category(1L, 0L, "Action"), category(2L, 1L, "Drama"), category(3L, 2L, "Later"))
    )

    private suspend fun FakeCategoryRepository.names() = getAll().map { it.name }

    @Test
    fun aCreatedCategoryGoesLast() = runBlocking {
        val repository = repository()

        val result = CreateCategoryWithName(repository).await("  Comedy ")

        assertTrue(result is CreateCategoryWithName.Result.Success)
        assertEquals(listOf("Action", "Drama", "Later", "Comedy"), repository.names())
    }

    @Test
    fun aCategoryNeedsANewNonBlankName() = runBlocking {
        val repository = repository()
        val create = CreateCategoryWithName(repository)

        assertEquals(CreateCategoryWithName.Result.EmptyName, create.await("   "))
        assertEquals(CreateCategoryWithName.Result.NameAlreadyExists, create.await("Drama"))
        assertEquals(3, repository.getAll().size)
    }

    @Test
    fun renamingKeepsTheNamesUnique() = runBlocking {
        val repository = repository()
        val rename = RenameCategory(repository)

        assertEquals(RenameCategory.Result.NameAlreadyExists, rename.await(1L, "Drama"))
        // Its own name is no conflict
        assertEquals(RenameCategory.Result.Success, rename.await(1L, "Action"))
        assertEquals(RenameCategory.Result.Success, rename.await(1L, "Fights"))
        assertEquals(listOf("Fights", "Drama", "Later"), repository.names())
    }

    @Test
    fun reorderingMovesACategory() = runBlocking {
        val repository = repository()
        val reorder = ReorderCategory(repository)

        assertEquals(ReorderCategory.Result.Success, reorder.await(3L, 0))
        assertEquals(listOf("Later", "Action", "Drama"), repository.names())
        assertEquals(listOf(0L, 1L, 2L), repository.getAll().map { it.order })

        assertEquals(ReorderCategory.Result.Unchanged, reorder.await(3L, 5))
        assertEquals(ReorderCategory.Result.Unchanged, reorder.await(42L, 0))
    }

    @Test
    fun deletingACategoryMovesItsAnimeToTheDefaultOne() = runBlocking {
        val repository = repository()
        // Anime 10 only in Drama, anime 11 in Drama and Later
        repository.setAnimeCategories(10L, listOf(2L))
        repository.setAnimeCategories(11L, listOf(2L, 3L))

        val result = DeleteCategory(repository, InMemoryDataStore()).await(2L)

        assertEquals(DeleteCategory.Result.Success, result)
        assertEquals(emptyList<Category>(), repository.getCategoriesByAnimeId(10L))
        assertEquals(listOf("Later"), repository.getCategoriesByAnimeId(11L).map { it.name })
        // No gap left in the order
        assertEquals(listOf("Action" to 0L, "Later" to 1L), repository.getAll().map { it.name to it.order })
    }

    @Test
    fun deletingACategoryForgetsItInTheSettings() = runBlocking {
        val dataStore = InMemoryDataStore()
        dataStore.edit {
            it[LibraryPreferences.DEFAULT_CATEGORY] = 2
            it[LibraryPreferences.UPDATE_CATEGORIES] = setOf("1", "2")
            it[LibraryPreferences.UPDATE_CATEGORIES_EXCLUDE] = setOf("2")
        }

        DeleteCategory(repository(), dataStore).await(2L)

        val prefs = LibraryPreferences.transform(dataStore.data.first())
        assertEquals(LibraryPreferences.DEFAULT_CATEGORY_ASK, prefs.defaultCategory)
        assertEquals(setOf("1"), prefs.updateCategories)
        assertEquals(emptySet<String>(), prefs.updateCategoriesExclude)
    }

    @Test
    fun deletingAnotherCategoryKeepsTheDefaultCategorySetting() = runBlocking {
        val dataStore = InMemoryDataStore()
        dataStore.edit { it[LibraryPreferences.DEFAULT_CATEGORY] = 1 }

        DeleteCategory(repository(), dataStore).await(2L)

        assertEquals(1, LibraryPreferences.transform(dataStore.data.first()).defaultCategory)
    }

    @Test
    fun settingCategoriesInBulkKeepsTheOtherOnes() = runBlocking {
        val repository = repository()
        repository.setAnimeCategories(10L, listOf(1L, 2L))
        repository.setAnimeCategories(11L, listOf(3L))

        SetAnimeCategories(repository).await(listOf(10L, 11L), add = listOf(3L), remove = listOf(2L))

        assertEquals(listOf(1L, 3L), repository.getCategoriesByAnimeId(10L).map { it.id })
        assertEquals(listOf(3L), repository.getCategoriesByAnimeId(11L).map { it.id })
    }

    @Test
    fun theDefaultCategorySortGoesToItsSetting() = runBlocking {
        val repository = repository()
        val dataStore = InMemoryDataStore()
        val setSort = SetCategorySort(repository, dataStore)

        setSort.await(2L, 0x2L)
        setSort.await(Category.UNCATEGORIZED_ID, 0x80000004L)

        assertEquals(listOf(0L, 0x2L, 0L), repository.getAll().map { it.flags })
        assertEquals(0x80000004L, LibraryPreferences.transform(dataStore.data.first()).defaultCategorySortFlags)
    }
}
