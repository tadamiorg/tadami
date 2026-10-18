package com.sf.tadami.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.data.interactors.category.CreateCategoryWithName
import com.sf.tadami.data.interactors.category.DeleteCategory
import com.sf.tadami.data.interactors.category.GetCategories
import com.sf.tadami.data.interactors.category.RenameCategory
import com.sf.tadami.data.interactors.category.ReorderCategory
import com.sf.tadami.domain.category.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Why a name was refused, or the change failed */
enum class CategoryError { EMPTY_NAME, NAME_EXISTS, INTERNAL }

sealed interface CategoryDialog {
    val error: CategoryError?

    data class Create(override val error: CategoryError? = null) : CategoryDialog
    data class Rename(val category: Category, override val error: CategoryError? = null) : CategoryDialog
    data class Delete(val category: Category, override val error: CategoryError? = null) : CategoryDialog
}

class CategoryViewModel : ViewModel() {
    private val getCategories: GetCategories = Injekt.get()
    private val createCategoryWithName: CreateCategoryWithName = Injekt.get()
    private val renameCategory: RenameCategory = Injekt.get()
    private val deleteCategory: DeleteCategory = Injekt.get()
    private val reorderCategory: ReorderCategory = Injekt.get()

    // The user's categories by order, null until read
    val categories: StateFlow<List<Category>?> = getCategories.subscribe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _dialog: MutableStateFlow<CategoryDialog?> = MutableStateFlow(null)
    val dialog = _dialog.asStateFlow()

    fun showDialog(dialog: CategoryDialog) {
        _dialog.update { dialog }
    }

    fun dismissDialog() {
        _dialog.update { null }
    }

    // The name changed: the error was about the previous one
    fun clearDialogError() {
        _dialog.update { dialog ->
            when (dialog) {
                is CategoryDialog.Create -> dialog.copy(error = null)
                is CategoryDialog.Rename -> dialog.copy(error = null)
                is CategoryDialog.Delete -> dialog.copy(error = null)
                null -> null
            }
        }
    }

    fun createCategory(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val error = when (createCategoryWithName.await(name)) {
                is CreateCategoryWithName.Result.Success -> null
                CreateCategoryWithName.Result.EmptyName -> CategoryError.EMPTY_NAME
                CreateCategoryWithName.Result.NameAlreadyExists -> CategoryError.NAME_EXISTS
                is CreateCategoryWithName.Result.InternalError -> CategoryError.INTERNAL
            }
            closeOrShow(error) { CategoryDialog.Create(error) }
        }
    }

    fun renameCategory(category: Category, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val error = when (renameCategory.await(category.id, name)) {
                RenameCategory.Result.Success -> null
                RenameCategory.Result.EmptyName -> CategoryError.EMPTY_NAME
                RenameCategory.Result.NameAlreadyExists -> CategoryError.NAME_EXISTS
                is RenameCategory.Result.InternalError -> CategoryError.INTERNAL
            }
            closeOrShow(error) { CategoryDialog.Rename(category, error) }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch(Dispatchers.IO) {
            val error = when (deleteCategory.await(category.id)) {
                DeleteCategory.Result.Success -> null
                is DeleteCategory.Result.InternalError -> CategoryError.INTERNAL
            }
            closeOrShow(error) { CategoryDialog.Delete(category, error) }
        }
    }

    fun moveCategory(category: Category, up: Boolean) {
        val newIndex = categories.value.orEmpty().moveIndex(category.id, up) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            reorderCategory.await(category.id, newIndex)
        }
    }

    private fun closeOrShow(error: CategoryError?, dialog: () -> CategoryDialog) {
        _dialog.update { if (error == null) null else dialog() }
    }
}

/** Where the category goes, one step [up] or down, null when it is already at that end */
internal fun List<Category>.moveIndex(categoryId: Long, up: Boolean): Int? {
    val index = indexOfFirst { it.id == categoryId }
    if (index < 0) return null
    return (if (up) index - 1 else index + 1).takeIf { it in indices }
}
