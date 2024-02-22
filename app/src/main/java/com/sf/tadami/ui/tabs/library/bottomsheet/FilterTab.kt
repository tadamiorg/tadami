package com.sf.tadami.ui.tabs.library.bottomsheet

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import com.sf.tadami.R
import com.sf.tadami.preferences.library.LibraryFilter
import com.sf.tadami.ui.components.filters.TriStateItem

@Composable
fun FilterTab(filters: LibraryFilter, setFilters: (filterFlags: Long) -> Unit) {
    Column {
        TriStateItem(
            label = stringResource(id = R.string.filter_unseen),
            state = filters.readState
        ) {
            val newFilters = when (filters.readState) {
                ToggleableState.On -> filters.flags.setFlags(
                    LibraryFilter.READ,
                    LibraryFilter.READ_MASK
                )

                ToggleableState.Off -> filters.flags.setFlags(0L, LibraryFilter.READ_MASK)
                else -> filters.flags.setFlags(LibraryFilter.UNREAD, LibraryFilter.READ_MASK)
            }
            setFilters(newFilters)
        }

        TriStateItem(
            label = stringResource(id = R.string.filter_started),
            state = filters.startedState
        ) {
            val newFilters = when (filters.startedState) {
                ToggleableState.On -> filters.flags.setFlags(
                    LibraryFilter.UNSTARTED,
                    LibraryFilter.STARTED_MASK
                )

                ToggleableState.Off -> filters.flags.setFlags(0L, LibraryFilter.STARTED_MASK)
                else -> filters.flags.setFlags(LibraryFilter.STARTED, LibraryFilter.STARTED_MASK)
            }
            setFilters(newFilters)
        }
    }
}