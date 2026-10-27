package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.tracksselection.tabs

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.sf.tadami.source.model.Track
import com.sf.tadami.ui.components.modal.PlayerChoiceRow
import com.sf.tadami.ui.components.widgets.FastScrollLazyColumn

/** The subtitle tracks by language, numbered when a language has several, scrolled to [selectedSubtitleTrack]. */
@Composable
fun SubtitlesTab(
    subtitleTracks: List<Track.SubtitleTrack>,
    selectedSubtitleTrack: Track.SubtitleTrack?,
    onSelect: (Track.SubtitleTrack) -> Unit,
) {
    val listState = rememberLazyListState()

    val trackDisplayNames = remember(subtitleTracks) {
        subtitleTracks.groupBy { it.lang }
            .flatMap { (lang, tracks) ->
                if (tracks.size > 1) {
                    // If there are multiple tracks with the same language, add numbers
                    tracks.mapIndexed { index, track ->
                        track to "$lang #${index + 1}"
                    }
                } else {
                    // If there's only one track with this language, use the language name as is
                    tracks.map { it to lang }
                }
            }.toMap()
    }

    LaunchedEffect(selectedSubtitleTrack, subtitleTracks) {
        listState.scrollToItem(selectedSubtitleTrack?.let { subtitleTracks.indexOf(it) }.takeIf { it != -1 } ?: 0)
    }

    FastScrollLazyColumn(
        thumbAlways = true,
        state = listState
    ) {
        items(subtitleTracks) { track ->
            PlayerChoiceRow(
                selected = track == selectedSubtitleTrack,
                label = trackDisplayNames[track] ?: track.lang,
                onClick = { onSelect(track) },
            )
        }
    }
}
