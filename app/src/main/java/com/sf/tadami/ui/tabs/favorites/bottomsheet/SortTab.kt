package com.sf.tadami.ui.tabs.favorites.bottomsheet

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.ui.components.filters.SortItem
import com.sf.tadami.ui.tabs.settings.screens.library.LibrarySort
import com.sf.tadami.ui.tabs.settings.screens.library.LibrarySort.SortType

@Composable
fun SortTab(
    sort: LibrarySort,
    onItemSelected: (Long) -> Unit,
) {
    SortItem(
        label = stringResource(id = R.string.library_sheet_sort_alphabet),
        sortDescending = sort.isAscending.takeIf { sort.sortType is SortType.Alphabetical }
    ) {
        onItemSelected(SortType.Alphabetical.flag)
    }
    SortItem(
        label = stringResource(id = R.string.library_sheet_sort_unseen_count),
        sortDescending = sort.isAscending.takeIf { sort.sortType is SortType.UnseenCount }
    ) {
        onItemSelected(SortType.UnseenCount.flag)
    }
    SortItem(
        label = stringResource(id = R.string.library_sheet_sort_episode_count),
        sortDescending = sort.isAscending.takeIf { sort.sortType is SortType.EpisodeCount }
    ) {
        onItemSelected(SortType.EpisodeCount.flag)
    }
}