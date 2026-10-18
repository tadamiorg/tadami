package com.sf.tadami.ui.tabs.library.bottomsheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sf.tadami.R
import com.sf.tadami.domain.category.Category
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.ui.components.dialog.sheets.TabContent
import com.sf.tadami.ui.components.dialog.sheets.TabbedBottomSheet
import com.sf.tadami.ui.tabs.library.categorySort
import com.sf.tadami.ui.tabs.library.select

fun Long.setFlags(flag: Long, mask: Long): Long {
    return this and mask.inv() or flag
}

/**
 * The filters, sort and display of the library. With the per category sort, the sort is the one of the
 * [activeCategory] tab, [onCategorySortSelected] saves it.
 */
@Composable
fun LibrarySheetContent(
    activeCategory: Category?,
    onCategorySortSelected: (category: Category, sortFlags: Long) -> Unit,
) {
    val libraryPreferencesStore = rememberDataStoreState(customPrefs = LibraryPreferences)
    val libraryPreferencesState by libraryPreferencesStore.value.collectAsState()
    val categorySortCategory = activeCategory?.takeIf { libraryPreferencesState.categorizedSort }

    TabbedBottomSheet(
        tabs = listOf(
            TabContent(
                titleRes = R.string.action_filter
            ) {
                FilterTab(
                    filters = libraryPreferencesState.filterFlags,
                    setFilters = {
                        libraryPreferencesStore.setValue(
                            libraryPreferencesState.copy(
                                filterFlags = libraryPreferencesState.filterFlags.copy(
                                    flags = it
                                )
                            )
                        )
                    }
                )
            },
            TabContent(
                titleRes = R.string.action_sort
            ) {
                val sort = categorySortCategory?.let {
                    categorySort(
                        category = it,
                        perCategory = true,
                        librarySort = libraryPreferencesState.sortFlags,
                        defaultCategoryFlags = libraryPreferencesState.defaultCategorySortFlags
                    )
                } ?: libraryPreferencesState.sortFlags
                SortTab(sort = sort,
                    onItemSelected = {
                        val newSort = sort.select(it)
                        if (categorySortCategory != null) {
                            onCategorySortSelected(categorySortCategory, newSort.flags)
                        } else {
                            libraryPreferencesStore.setValue(
                                libraryPreferencesState.copy(sortFlags = newSort)
                            )
                        }
                    }
                )
            },
            TabContent(
                titleRes = R.string.title_display
            ) {
                DisplayTab(
                    showCategoryTabs = libraryPreferencesState.showCategoryTabs,
                    setShowCategoryTabs = {
                        libraryPreferencesStore.setValue(
                            libraryPreferencesState.copy(showCategoryTabs = it)
                        )
                    },
                    showNumberOfItems = libraryPreferencesState.showCategoryNumberOfItems,
                    setShowNumberOfItems = {
                        libraryPreferencesStore.setValue(
                            libraryPreferencesState.copy(showCategoryNumberOfItems = it)
                        )
                    }
                )
            }
        )
    )
}