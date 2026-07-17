package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.ui.animeinfos.details.episodes.EpisodeListItem
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogCancelButton
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton
import com.sf.tadami.ui.components.dialog.simple.SimpleDialog
import com.sf.tadami.ui.components.widgets.FastScrollLazyColumn
import com.sf.tadami.ui.utils.padding
import com.sf.tadami.ui.utils.toRelativeString
import java.util.Date

@Composable
fun EpisodesDialog(
    opened : Boolean,
    episodes : List<Episode>,
    displayMode: Anime.DisplayMode?,
    initialEpisode : Episode?,
    onConfirm : (Episode) -> Unit,
    onDismissRequest : () -> Unit

) {
    var selectedEpisode by remember { mutableStateOf(initialEpisode) }
    val listState = rememberLazyListState()

    // Distinct seasons present among the loaded episodes, ordered by season number.
    val seasons = remember(episodes) {
        episodes.asSequence()
            .filter { it.seasonName != null }
            .distinctBy { it.seasonName }
            .sortedBy { it.seasonNumber ?: 0f }
            .map { it.seasonName!! }
            .toList()
    }
    var selectedSeasonName by remember(initialEpisode) { mutableStateOf(initialEpisode?.seasonName) }

    val displayedEpisodes = remember(episodes, selectedSeasonName, seasons) {
        if (seasons.isNotEmpty() && selectedSeasonName != null) {
            episodes.filter { it.seasonName == selectedSeasonName }
        } else {
            episodes
        }
    }

    SimpleDialog(
        opened = opened,
        title = {
                Text(text = stringResource(id = R.string.label_episodes))
        },
        confirmButton = {
            DefaultDialogConfirmButton(
                enabled = selectedEpisode != initialEpisode
            ) {
                if(selectedEpisode != null){
                    onConfirm(selectedEpisode!!)
                    onDismissRequest()
                }
            }
        },
        onDismissRequest = {
            onDismissRequest()
        },
        dismissButton = {
            DefaultDialogCancelButton()
        }
    ) {
        LaunchedEffect(displayedEpisodes){
            val target = displayedEpisodes.indexOfFirst { it.id == initialEpisode?.id }
            listState.animateScrollToItem(target.takeIf { it != -1 } ?: 0)
        }
        FastScrollLazyColumn(thumbAlways = true, noEndPadding = true, state = listState){
            if (seasons.size > 1) {
                item(key = "season_selector") {
                    SeasonDropdownRow(
                        seasons = seasons,
                        selectedSeasonName = selectedSeasonName,
                        onSeasonSelected = { selectedSeasonName = it }
                    )
                }
            }
            items(displayedEpisodes){episode ->
                EpisodeListItem(
                    title =  when(displayMode){
                        is Anime.DisplayMode.NAME -> episode.name
                        else -> "${stringResource(id = R.string.player_screen_episode_label)} ${episode.episodeNumber}"
                    },
                    onClick = {
                        selectedEpisode = episode
                    },
                    onLongClick = {

                    },
                    seen = episode.seen,
                    selected = selectedEpisode?.id == episode.id,
                    watchProgress = null,
                    date = when {
                        episode.dateUpload > 0L -> {
                            Date(episode.dateUpload).toRelativeString(LocalContext.current)
                        }
                        episode.dateFetch > 0L -> {
                            Date(episode.dateFetch).toRelativeString(LocalContext.current)
                        }
                        else -> null
                    },
                    languages = null,
                )
            }
        }
    }
}

@Composable
private fun SeasonDropdownRow(
    seasons: List<String>,
    selectedSeasonName: String?,
    onSeasonSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.tiny
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = selectedSeasonName ?: "",
            style = MaterialTheme.typography.titleMedium
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            seasons.forEach { season ->
                DropdownMenuItem(
                    text = { Text(season) },
                    onClick = {
                        expanded = false
                        onSeasonSelected(season)
                    }
                )
            }
        }
    }
}
