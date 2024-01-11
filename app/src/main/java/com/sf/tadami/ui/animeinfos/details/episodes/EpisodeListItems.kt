package com.sf.tadami.ui.animeinfos.details.episodes

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastAny
import com.sf.tadami.R
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.ui.animeinfos.details.DetailsScreenItem
import com.sf.tadami.ui.components.data.EpisodeItem
import com.sf.tadami.ui.utils.formatMinSec
import com.sf.tadami.ui.utils.toRelativeString
import java.util.Date

fun LazyListScope.episodeItems(
    displayMode : Anime.DisplayMode? = Anime.DisplayMode.NAME,
    episodes: List<EpisodeItem>,
    onEpisodeClicked: (epId: Long) -> Unit,
    onEpisodeSelected: (episode: EpisodeItem, selected: Boolean) -> Unit
) {


    items(
        items = episodes,
        key = { it.episode.id },
        contentType = { DetailsScreenItem.EPISODE }) { episodeItem ->
        val context = LocalContext.current
        EpisodeListItem(
            title = remember(displayMode) {
                when(displayMode){
                    is Anime.DisplayMode.NAME -> episodeItem.episode.name
                    is Anime.DisplayMode.NUMBER -> "${context.getString(R.string.player_screen_episode_label)} ${episodeItem.episode.episodeNumber}"
                    else -> episodeItem.episode.name
                }
            },
            onClick = {
                updateSelected(
                    episodeItem = episodeItem,
                    episodes = episodes,
                    onEpisodeSelected = onEpisodeSelected,
                    onEpisodeClicked = onEpisodeClicked
                )
            },
            onLongClick = {
                updateSelected(
                    episodeItem = episodeItem,
                    episodes = episodes,
                    onEpisodeSelected = onEpisodeSelected,
                    onEpisodeLongClicked = true
                )
            },
            seen = episodeItem.episode.seen,
            watchProgress = episodeItem.episode.timeSeen
                .takeIf { !episodeItem.episode.seen && episodeItem.episode.totalTime > 0L && it > 0L }
                ?.let {
                    "${it.formatMinSec()}/${episodeItem.episode.totalTime.formatMinSec()}"
                },
            date = when {
                episodeItem.episode.dateUpload > 0L -> {
                    Date(episodeItem.episode.dateUpload).toRelativeString(LocalContext.current)
                }
                episodeItem.episode.dateFetch > 0L -> {
                    Date(episodeItem.episode.dateFetch).toRelativeString(LocalContext.current)
                }
                else -> null
            },
            selected = episodeItem.selected,
            languages = episodeItem.episode.languages
        )
    }
}

fun updateSelected(
    episodeItem: EpisodeItem,
    episodes: List<EpisodeItem>,
    onEpisodeSelected: (episodeItem: EpisodeItem, selected: Boolean) -> Unit,
    onEpisodeClicked: ((episodeId: Long) -> Unit)? = null,
    onEpisodeLongClicked: Boolean = false
) {
    onEpisodeClicked?.let{ clicked ->
        when {
            episodeItem.selected -> {
                onEpisodeSelected(episodeItem, false)
            }
            episodes.fastAny { it.selected } -> {
                onEpisodeSelected(episodeItem, true)
            }
            else -> {
                clicked.invoke(episodeItem.episode.id)
            }
        }
    }
    if(!onEpisodeLongClicked) return
    onEpisodeSelected(episodeItem, !episodeItem.selected)
}