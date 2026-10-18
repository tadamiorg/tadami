package com.sf.tadami.data.interactors.category

import android.util.Log
import com.sf.tadami.data.category.CategoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ReorderCategory(
    private val categoryRepository: CategoryRepository,
) {
    // Two moves at once would both start from the same order
    private val mutex = Mutex()

    /** Moves the category to [newIndex] in the order */
    suspend fun await(categoryId: Long, newIndex: Int): Result = mutex.withLock {
        try {
            val categories = categoryRepository.getAll().toMutableList()
            val currentIndex = categories.indexOfFirst { it.id == categoryId }
            if (currentIndex == -1 || newIndex !in categories.indices || newIndex == currentIndex) {
                return@withLock Result.Unchanged
            }

            categories.add(newIndex, categories.removeAt(currentIndex))
            categoryRepository.updateAllOrders(categories.map { it.id })
            Result.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d("ReorderCategory", e.stackTraceToString())
            Result.InternalError(e)
        }
    }

    sealed interface Result {
        data object Success : Result
        data object Unchanged : Result
        data class InternalError(val error: Throwable) : Result
    }
}
