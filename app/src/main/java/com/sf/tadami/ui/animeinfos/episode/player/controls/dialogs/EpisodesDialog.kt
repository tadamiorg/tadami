package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.ui.components.modal.PlayerChoiceRow
import com.sf.tadami.ui.components.modal.PlayerPanel
import com.sf.tadami.ui.components.widgets.FastScrollLazyColumn
import com.sf.tadami.ui.utils.toRelativeString
import java.util.Date

/** The episodes, scrolled to the current one, the seen ones dimmed: a tap plays it and closes the panel. */
@Composable
fun EpisodesDialog(
    opened : Boolean,
    episodes : List<Episode>,
    displayMode: Anime.DisplayMode?,
    initialEpisode : Episode?,
    onConfirm : (Episode) -> Unit,
    onDismissRequest : () -> Unit

) {
    PlayerPanel(
        visible = opened,
        onDismissRequest = onDismissRequest,
        title = stringResource(id = R.string.label_episodes),
    ) {
        val listState = rememberLazyListState()
        LaunchedEffect(Unit){
            // Match by id: the list re-emits updated copies (progress, seen) that no longer equal the current episode.
            listState.scrollToItem(initialEpisode?.let { episodes.indexOfFirst { ep -> ep.id == it.id } }.takeIf { it !=-1 } ?: 0)
        }
        FastScrollLazyColumn(thumbAlways = true, state = listState){
            items(episodes){episode ->
                PlayerChoiceRow(
                    selected = initialEpisode?.id == episode.id,
                    label = when(displayMode){
                        is Anime.DisplayMode.NAME -> episode.name
                        else -> "${stringResource(id = R.string.player_screen_episode_label)} ${episode.episodeNumber}"
                    },
                    supporting = when {
                        episode.dateUpload > 0L -> {
                            Date(episode.dateUpload).toRelativeString(LocalContext.current)
                        }
                        episode.dateFetch > 0L -> {
                            Date(episode.dateFetch).toRelativeString(LocalContext.current)
                        }
                        else -> null
                    },
                    dimmed = episode.seen,
                    onClick = {
                        if (episode.id != initialEpisode?.id) onConfirm(episode)
                        onDismissRequest()
                    },
                )
            }
        }
    }
}
