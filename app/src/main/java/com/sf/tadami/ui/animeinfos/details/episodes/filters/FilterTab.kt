package com.sf.tadami.ui.animeinfos.details.episodes.filters

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import com.sf.tadami.R
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.ui.components.filters.TriStateItem
import com.sf.tadami.ui.tabs.library.bottomsheet.setFlags

@Composable
fun FilterTab(anime: Anime, setFilters: (filterFlags: Long) -> Unit) {
    Column {
        TriStateItem(
            label = stringResource(id = R.string.filter_unseen),
            state = anime.unseenFilter
        ) {
            val newFilters = when (anime.unseenFilter) {
                ToggleableState.On -> anime.episodeFlags.setFlags(
                    Anime.EPISODE_SHOW_SEEN,
                    Anime.EPISODE_UNSEEN_MASK
                )

                ToggleableState.Off -> anime.episodeFlags.setFlags(
                    Anime.SHOW_ALL,
                    Anime.EPISODE_UNSEEN_MASK
                )

                else -> anime.episodeFlags.setFlags(
                    Anime.EPISODE_SHOW_UNSEEN,
                    Anime.EPISODE_UNSEEN_MASK
                )
            }
            setFilters(newFilters)
        }
    }
}