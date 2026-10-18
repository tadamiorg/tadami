package com.sf.tadami.data.interactors.category

import android.util.Log
import com.sf.tadami.data.category.CategoryRepository
import kotlinx.coroutines.CancellationException

class CreateCategoryWithName(
    private val categoryRepository: CategoryRepository,
) {

    suspend fun await(name: String): Result {
        val categoryName = name.trim()
        return try {
            val categories = categoryRepository.getAll()
            when {
                categoryName.isEmpty() -> Result.EmptyName
                categories.any { it.name == categoryName } -> Result.NameAlreadyExists
                else -> Result.Success(categoryRepository.insert(categoryName, flags = 0L))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d("CreateCategoryWithName", e.stackTraceToString())
            Result.InternalError(e)
        }
    }

    sealed interface Result {
        data class Success(val categoryId: Long) : Result
        data object EmptyName : Result
        data object NameAlreadyExists : Result
        data class InternalError(val error: Throwable) : Result
    }
}
