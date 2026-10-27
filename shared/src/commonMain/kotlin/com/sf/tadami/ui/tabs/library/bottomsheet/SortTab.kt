package com.sf.tadami.ui.tabs.library.bottomsheet

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.preferences.library.LibrarySort
import com.sf.tadami.preferences.library.LibrarySort.SortType
import com.sf.tadami.ui.components.filters.SortItem

@Composable
fun SortTab(
    sort: LibrarySort,
    onItemSelected: (Long) -> Unit,
) {
    Column {
        SortItem(
            label = stringResource(Res.string.filter_sort_alphabet),
            sortDescending = (!sort.isAscending).takeIf { sort.sortType is SortType.Alphabetical }
        ) {
            onItemSelected(SortType.Alphabetical.flag)
        }
        SortItem(
            label = stringResource(Res.string.filter_sort_unseen_count),
            sortDescending = (!sort.isAscending).takeIf { sort.sortType is SortType.UnseenCount }
        ) {
            onItemSelected(SortType.UnseenCount.flag)
        }
        SortItem(
            label = stringResource(Res.string.filter_sort_episode_count),
            sortDescending = (!sort.isAscending).takeIf { sort.sortType is SortType.EpisodeCount }
        ) {
            onItemSelected(SortType.EpisodeCount.flag)
        }
    }
}