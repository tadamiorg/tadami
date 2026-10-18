package com.sf.tadami.data.category

import com.sf.tadami.data.DataBaseHandler
import com.sf.tadami.domain.category.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    suspend fun getAll(): List<Category>

    fun getAllAsFlow(): Flow<List<Category>>

    suspend fun getCategoriesByAnimeId(animeId: Long): List<Category>

    fun getCategoriesByAnimeIdAsFlow(animeId: Long): Flow<List<Category>>

    /** Adds the category last in the order, returns its id */
    suspend fun insert(name: String, flags: Long): Long

    suspend fun updateName(categoryId: Long, name: String)

    /** Orders the categories as [orderedIds], the ones missing from it after them */
    suspend fun updateAllOrders(orderedIds: List<Long>)

    suspend fun delete(categoryId: Long)

    /** Replaces the categories of the anime, none puts it in the Default category */
    suspend fun setAnimeCategories(animeId: Long, categoryIds: Collection<Long>)

    /** Adds the anime to [add] and removes them from [remove], their other categories stay */
    suspend fun updateAnimeCategories(animeIds: Collection<Long>, add: Collection<Long>, remove: Collection<Long>)
}

class CategoryRepositoryImpl(
    private val handler: DataBaseHandler,
) : CategoryRepository {

    override suspend fun getAll(): List<Category> {
        return handler.awaitList { categoriesQueries.getCategories(CategoryMapper::mapCategory) }
    }

    override fun getAllAsFlow(): Flow<List<Category>> {
        return handler.subscribeToList { categoriesQueries.getCategories(CategoryMapper::mapCategory) }
    }

    override suspend fun getCategoriesByAnimeId(animeId: Long): List<Category> {
        return handler.awaitList {
            categoriesQueries.getCategoriesByAnimeId(animeId, CategoryMapper::mapCategory)
        }
    }

    override fun getCategoriesByAnimeIdAsFlow(animeId: Long): Flow<List<Category>> {
        return handler.subscribeToList {
            categoriesQueries.getCategoriesByAnimeId(animeId, CategoryMapper::mapCategory)
        }
    }

    override suspend fun insert(name: String, flags: Long): Long {
        return handler.await {
            categoriesQueries.insert(name = name, flags = flags)
            categoriesQueries.selectLastInsertedRowId().executeAsOne()
        }
    }

    override suspend fun updateName(categoryId: Long, name: String) {
        handler.await { categoriesQueries.updateName(name = name, categoryId = categoryId) }
    }

    override suspend fun updateAllOrders(orderedIds: List<Long>) {
        handler.await {
            val current = categoriesQueries.getCategories().executeAsList().map { it._id }
            val ids = orderedIds.filter { it in current } + current.filterNot { it in orderedIds }
            ids.forEachIndexed { index, categoryId ->
                categoriesQueries.updateSort(sort = index.toLong(), categoryId = categoryId)
            }
        }
    }

    override suspend fun delete(categoryId: Long) {
        handler.await { categoriesQueries.delete(categoryId) }
    }

    override suspend fun setAnimeCategories(animeId: Long, categoryIds: Collection<Long>) {
        handler.await {
            animeCategoriesQueries.deleteByAnimeId(animeId)
            categoryIds.filterNot { it == Category.UNCATEGORIZED_ID }.forEach { categoryId ->
                animeCategoriesQueries.insert(animeId = animeId, categoryId = categoryId)
            }
        }
    }

    override suspend fun updateAnimeCategories(
        animeIds: Collection<Long>,
        add: Collection<Long>,
        remove: Collection<Long>
    ) {
        val toAdd = add.filterNot { it == Category.UNCATEGORIZED_ID }
        val toRemove = remove.filterNot { it == Category.UNCATEGORIZED_ID }
        if (toAdd.isEmpty() && toRemove.isEmpty()) return
        handler.await {
            animeIds.forEach { animeId ->
                if (toRemove.isNotEmpty()) {
                    animeCategoriesQueries.deleteByAnimeIdAndCategoryIds(animeId, toRemove)
                }
                toAdd.forEach { categoryId ->
                    animeCategoriesQueries.insert(animeId = animeId, categoryId = categoryId)
                }
            }
        }
    }
}
