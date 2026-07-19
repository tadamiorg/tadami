package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.audioselection.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sf.tadami.R
import com.sf.tadami.source.model.Track
import com.sf.tadami.ui.components.screens.ScreenTabContent
import com.sf.tadami.ui.components.widgets.FastScrollLazyColumn

@Composable()
fun audioTab(
    audioTracks: List<Track.AudioTrack>,
    selectedAudioTrack: Track.AudioTrack? = null,
    selectedOption: Track.AudioTrack?,
    onOptionSelected: (Track.AudioTrack?) -> Unit
) : ScreenTabContent {

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

    return ScreenTabContent(
        titleRes = R.string.label_audio,
    ){ contentPadding: PaddingValues, _ ->
        LaunchedEffect(Unit){
            val real = selectedAudioTrack ?: audioTracks.firstOrNull()
            onOptionSelected(real)
            listState.animateScrollToItem(real?.let { audioTracks.indexOf(it) }.takeIf { it != -1 } ?: 0)
        }
        FastScrollLazyColumn(
            modifier = Modifier.padding(contentPadding),
            thumbAlways = true,
            state = listState
        ) {
            items(audioTracks) { track ->
                Row(
                    modifier = Modifier
                        .defaultMinSize(1.dp, 1.dp)
                        .fillMaxWidth()
                        .selectable(
                            selected = (track == selectedOption),
                            onClick = {
                                onOptionSelected(track)
                            }
                        ),
                    horizontalArrangement = Arrangement.Start
                ) {
                    RadioButton(modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .defaultMinSize(1.dp, 1.dp),
                        selected = (track == selectedOption),
                        onClick = {
                            onOptionSelected(track)
                        }
                    )
                    Text(
                        text = trackDisplayNames[track] ?: track.lang,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        }
    }
}
