package com.sf.tadami.ui.tabs.library.bottomsheet

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import com.sf.tadami.R
import com.sf.tadami.ui.components.filters.TabbedBottomSheetContentPadding
import com.sf.tadami.ui.tabs.settings.model.rememberDataStoreState
import com.sf.tadami.ui.tabs.settings.screens.library.LibraryPreferences
import com.sf.tadami.ui.tabs.settings.screens.library.LibrarySort.Companion.SORT_DIRECTION
import com.sf.tadami.ui.tabs.settings.screens.library.LibrarySort.Companion.SORT_DIRECTION_MASK
import com.sf.tadami.ui.tabs.settings.screens.library.LibrarySort.Companion.SORT_TYPE_MASK
import kotlinx.coroutines.launch

fun Long.setFlags(flag: Long, mask: Long): Long {
    return this and mask.inv() or flag
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibrarySheetContent() {
    val libraryPreferencesStore = rememberDataStoreState(customPrefs = LibraryPreferences)
    val libraryPreferencesState by libraryPreferencesStore.value.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val pages = listOf(
        stringResource(id = R.string.library_sheet_filter),
        stringResource(id = R.string.library_sheet_sort)
    )
    val pagerState = rememberPagerState(pageCount = {
        pages.size
    })

    TabRow(
        selectedTabIndex = pagerState.currentPage,
    ) {
        pages.forEachIndexed { index, title ->
            Tab(
                modifier = Modifier.zIndex(6f),
                text = { Text(text = title) },
                selected = pagerState.currentPage == index,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
            )
        }
    }

    HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        state = pagerState,
    ) { page ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(TabbedBottomSheetContentPadding.Vertical)
        ) {
            when (page) {
                0 ->
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
                1 -> SortTab(sort = libraryPreferencesState.sortFlags,
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
        }
    }
}