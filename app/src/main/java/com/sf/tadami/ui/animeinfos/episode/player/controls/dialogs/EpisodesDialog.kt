package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        LaunchedEffect(Unit){
            val realEpisode = initialEpisode
            selectedEpisode = realEpisode
            listState.animateScrollToItem(realEpisode?.let { episodes.indexOf(it) }.takeIf { it !=-1 } ?: 0)
        }
        FastScrollLazyColumn(thumbAlways = true, noEndPadding = true, state = listState){
            items(episodes){episode ->
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