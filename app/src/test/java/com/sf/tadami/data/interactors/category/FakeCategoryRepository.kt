package com.sf.tadami.data.interactors.category

import com.sf.tadami.data.category.CategoryRepository
import com.sf.tadami.domain.category.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** The categories and the anime links in memory, as the database stores them */
class FakeCategoryRepository(categories: List<Category> = emptyList()) : CategoryRepository {

    val categories = MutableStateFlow(categories)
    val links = mutableSetOf<Pair<Long, Long>>()

    override suspend fun getAll(): List<Category> = categories.value.sortedWith(compareBy({ it.order }, { it.id }))

    override fun getAllAsFlow(): Flow<List<Category>> = categories.map { getAll() }

    override suspend fun getCategoriesByAnimeId(animeId: Long): List<Category> =
        getAll().filter { (animeId to it.id) in links }

    override fun getCategoriesByAnimeIdAsFlow(animeId: Long): Flow<List<Category>> =
        categories.map { getCategoriesByAnimeId(animeId) }

    override suspend fun insert(name: String, flags: Long): Long {
        val id = (categories.value.maxOfOrNull { it.id } ?: 0L) + 1
        val order = (categories.value.maxOfOrNull { it.order } ?: -1L) + 1
        categories.value += Category(id, name, order, flags)
        return id
    }

    override suspend fun updateName(categoryId: Long, name: String) {
        categories.value = categories.value.map { if (it.id == categoryId) it.copy(name = name) else it }
    }

    override suspend fun updateAllOrders(orderedIds: List<Long>) {
        val current = getAll().map { it.id }
        val ids = orderedIds.filter { it in current } + current.filterNot { it in orderedIds }
        categories.value = categories.value.map { it.copy(order = ids.indexOf(it.id).toLong()) }
    }

    override suspend fun delete(categoryId: Long) {
        categories.value = categories.value.filterNot { it.id == categoryId }
        links.removeAll { it.second == categoryId }
    }

    override suspend fun setAnimeCategories(animeId: Long, categoryIds: Collection<Long>) {
        links.removeAll { it.first == animeId }
        categoryIds.filterNot { it == Category.UNCATEGORIZED_ID }.forEach { links += animeId to it }
    }

    override suspend fun updateAnimeCategories(animeIds: Collection<Long>, add: Collection<Long>, remove: Collection<Long>) {
        animeIds.forEach { animeId ->
            links.removeAll { it.first == animeId && it.second in remove }
            add.filterNot { it == Category.UNCATEGORIZED_ID }.forEach { links += animeId to it }
        }
    }
}
