package com.sf.tadami.ui.tabs.library.bottomsheet


import androidx.compose.ui.state.ToggleableState
import com.sf.tadami.ui.components.data.LibraryItem
import com.sf.tadami.ui.tabs.settings.screens.library.LibraryFilter

fun List<LibraryItem>.libraryFilters(filters: LibraryFilter): List<LibraryItem> {
    return this.filter {
        when (filters.readState) {
            ToggleableState.On -> it.anime.unseenEpisodes > 0L
            ToggleableState.Off -> it.anime.unseenEpisodes == 0L
            else -> true
        }
    }.filter {
        when (filters.startedState) {
            ToggleableState.On -> it.anime.episodes != it.anime.unseenEpisodes
            ToggleableState.Off -> it.anime.episodes == it.anime.unseenEpisodes
            else -> true
        }
    }
}