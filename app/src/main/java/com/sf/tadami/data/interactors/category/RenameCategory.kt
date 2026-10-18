package com.sf.tadami.data.interactors.category

import android.util.Log
import com.sf.tadami.data.category.CategoryRepository
import kotlinx.coroutines.CancellationException

class RenameCategory(
    private val categoryRepository: CategoryRepository,
) {

    suspend fun await(categoryId: Long, name: String): Result {
        val categoryName = name.trim()
        return try {
            val categories = categoryRepository.getAll()
            when {
                categoryName.isEmpty() -> Result.EmptyName
                categories.any { it.id != categoryId && it.name == categoryName } -> Result.NameAlreadyExists
                else -> {
                    categoryRepository.updateName(categoryId, categoryName)
                    Result.Success
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d("RenameCategory", e.stackTraceToString())
            Result.InternalError(e)
        }
    }

    sealed interface Result {
        data object Success : Result
        data object EmptyName : Result
        data object NameAlreadyExists : Result
        data class InternalError(val error: Throwable) : Result
    }
}
