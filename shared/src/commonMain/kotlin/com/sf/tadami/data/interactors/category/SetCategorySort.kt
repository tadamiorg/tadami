package com.sf.tadami.data.interactors.category

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.sf.tadami.data.category.CategoryRepository
import com.sf.tadami.domain.category.Category
import com.sf.tadami.preferences.library.LibraryPreferences

/** The sort of one category, with the per category sort: in its flags, in a setting for the Default one */
class SetCategorySort(
    private val categoryRepository: CategoryRepository,
    private val dataStore: DataStore<Preferences>,
) {

    suspend fun await(categoryId: Long, sortFlags: Long) {
        if (categoryId == Category.UNCATEGORIZED_ID) {
            dataStore.edit { it[LibraryPreferences.DEFAULT_CATEGORY_FLAGS] = sortFlags }
        } else {
            categoryRepository.updateFlags(categoryId, sortFlags)
        }
    }
}
