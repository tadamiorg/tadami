package com.sf.tadami.ui.tabs.library.bottomsheet

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.state.ToggleableState
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.preferences.library.LibraryFilter
import com.sf.tadami.ui.components.filters.TriStateItem

@Composable
fun FilterTab(filters: LibraryFilter, setFilters: (filterFlags: Long) -> Unit) {
    Column {
        TriStateItem(
            label = stringResource(Res.string.filter_unseen),
            state = filters.seenState
        ) {
            val newFilters = when (filters.seenState) {
                ToggleableState.On -> filters.flags.setFlags(
                    LibraryFilter.SEEN,
                    LibraryFilter.SEEN_MASK
                )

                ToggleableState.Off -> filters.flags.setFlags(0L, LibraryFilter.SEEN_MASK)
                else -> filters.flags.setFlags(LibraryFilter.UNSEEN, LibraryFilter.SEEN_MASK)
            }
            setFilters(newFilters)
        }
        TriStateItem(
            label = stringResource(Res.string.filter_started),
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