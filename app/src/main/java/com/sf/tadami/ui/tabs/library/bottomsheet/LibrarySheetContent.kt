package com.sf.tadami.ui.tabs.library.bottomsheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.sf.tadami.R
import com.sf.tadami.ui.components.dialog.sheets.TabContent
import com.sf.tadami.ui.components.dialog.sheets.TabbedBottomSheet
import com.sf.tadami.ui.tabs.settings.model.rememberDataStoreState
import com.sf.tadami.ui.tabs.settings.screens.library.LibraryPreferences
import com.sf.tadami.ui.tabs.settings.screens.library.LibrarySort.Companion.SORT_DIRECTION
import com.sf.tadami.ui.tabs.settings.screens.library.LibrarySort.Companion.SORT_DIRECTION_MASK
import com.sf.tadami.ui.tabs.settings.screens.library.LibrarySort.Companion.SORT_TYPE_MASK

fun Long.setFlags(flag: Long, mask: Long): Long {
    return this and mask.inv() or flag
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LibrarySheetContent(
    sheetState : ModalBottomSheetState
) {
    val libraryPreferencesStore = rememberDataStoreState(customPrefs = LibraryPreferences)
    val libraryPreferencesState by libraryPreferencesStore.value.collectAsState()

    TabbedBottomSheet(
        sheetState = sheetState,
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
                SortTab(sort = libraryPreferencesState.sortFlags,
                    onItemSelected = {
                        val sortFlags = libraryPreferencesState.sortFlags
                        val sortType = sortFlags.sortType.flag
                        libraryPreferencesStore.setValue(
                            libraryPreferencesState.copy(
                                sortFlags = libraryPreferencesState.sortFlags.copy(
                                    flags = if(it == sortType){
                                        sortFlags.flags.setFlags(if(sortFlags.isAscending) 0L else SORT_DIRECTION, SORT_DIRECTION_MASK)
                                    }else{
                                        sortFlags.flags.setFlags(it,SORT_TYPE_MASK)
                                    }
                                )
                            )
                        )
                    }
                )
            }
        )
    )
}