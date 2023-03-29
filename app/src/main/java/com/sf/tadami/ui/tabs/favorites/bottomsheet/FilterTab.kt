package com.sf.tadami.ui.tabs.favorites.bottomsheet

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import com.sf.tadami.R
import com.sf.tadami.ui.components.filters.TriStateItem
import com.sf.tadami.ui.tabs.settings.screens.library.LibraryFilter

@Composable
fun FilterTab(filters: LibraryFilter, setFilters: (filterFlags: Long) -> Unit) {
    TriStateItem(
        label = stringResource(id = R.string.library_sheet_filter_unread),
        state = filters.readState
    ) {
        val newFilters = when (filters.readState) {
            ToggleableState.On -> filters.flags.setFlags(LibraryFilter.READ, LibraryFilter.READ_MASK)
            ToggleableState.Off -> filters.flags.setFlags(0L, LibraryFilter.READ_MASK)
            else -> filters.flags.setFlags(LibraryFilter.UNREAD, LibraryFilter.READ_MASK)
        }
        setFilters(newFilters)
    }

    TriStateItem(
        label = stringResource(id = R.string.library_sheet_filter_started),
        state = filters.startedState
    ) {
        val newFilters = when (filters.startedState) {
            ToggleableState.On -> filters.flags.setFlags(LibraryFilter.UNSTARTED, LibraryFilter.STARTED_MASK)
            ToggleableState.Off -> filters.flags.setFlags(0L, LibraryFilter.STARTED_MASK)
            else -> filters.flags.setFlags(LibraryFilter.STARTED, LibraryFilter.STARTED_MASK)
        }
        setFilters(newFilters)
    }
}