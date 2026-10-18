package com.sf.tadami.data.interactors.category

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.sf.tadami.data.category.CategoryRepository
import com.sf.tadami.preferences.library.LibraryPreferences
import kotlinx.coroutines.CancellationException

/** Deletes a category: its anime in no other category go back to the Default one, none is deleted */
class DeleteCategory(
    private val categoryRepository: CategoryRepository,
    private val dataStore: DataStore<Preferences>,
) {

    suspend fun await(categoryId: Long): Result {
        return try {
            categoryRepository.delete(categoryId)

            // The settings naming it forget it: added to the library, an anime goes through the dialog again
            val id = categoryId.toString()
            dataStore.edit { preferences ->
                if (preferences[LibraryPreferences.DEFAULT_CATEGORY]?.toLong() == categoryId) {
                    preferences.remove(LibraryPreferences.DEFAULT_CATEGORY)
                }
                listOf(LibraryPreferences.UPDATE_CATEGORIES, LibraryPreferences.UPDATE_CATEGORIES_EXCLUDE).forEach { key ->
                    val ids = preferences[key] ?: return@forEach
                    if (id in ids) preferences[key] = ids - id
                }
            }

            // No gap in the order
            categoryRepository.updateAllOrders(categoryRepository.getAll().map { it.id })
            Result.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d("DeleteCategory", e.stackTraceToString())
            Result.InternalError(e)
        }
    }

    sealed interface Result {
        data object Success : Result
        data class InternalError(val error: Throwable) : Result
    }
}
