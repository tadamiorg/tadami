package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.audioselection.tabs

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.sf.tadami.source.model.Track
import com.sf.tadami.ui.components.modal.PlayerChoiceRow
import com.sf.tadami.ui.components.widgets.FastScrollLazyColumn

/** The audio tracks by language, numbered when a language has several, scrolled to [selectedAudioTrack]. */
@Composable
fun AudioTab(
    audioTracks: List<Track.AudioTrack>,
    selectedAudioTrack: Track.AudioTrack?,
    onSelect: (Track.AudioTrack) -> Unit,
) {
    val listState = rememberLazyListState()

    val trackDisplayNames = remember(audioTracks) {
        audioTracks.groupBy { it.lang }
            .flatMap { (lang, tracks) ->
                if (tracks.size > 1) {
                    tracks.mapIndexed { index, track -> track to "$lang #${index + 1}" }
                } else {
                    tracks.map { it to lang }
                }
            }.toMap()
    }

    LaunchedEffect(Unit) {
        listState.scrollToItem(selectedAudioTrack?.let { audioTracks.indexOf(it) }.takeIf { it != -1 } ?: 0)
    }
    FastScrollLazyColumn(
        thumbAlways = true,
        state = listState
    ) {
        items(audioTracks) { track ->
            PlayerChoiceRow(
                selected = track == selectedAudioTrack,
                label = trackDisplayNames[track] ?: track.lang,
                onClick = { onSelect(track) },
            )
        }
    }
}
