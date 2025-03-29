package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.tracksselection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.sf.tadami.source.model.Track
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.tracksselection.tabs.subtitleSettingsTab
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.tracksselection.tabs.subtitlesTab
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton
import com.sf.tadami.ui.components.dialog.simple.TabbedSimpleDialog

@Composable
fun TracksSelectionDialog(
    opened: Boolean,
    subtitleTracks: List<Track.SubtitleTrack>? = null,
    audioTracks: List<Track.AudioTrack>? = null,
    selectedSubtitleTrack: Track.SubtitleTrack? = null,
    selectedAudioTrack: Track.AudioTrack? = null,
    onSubtitleTrackSelected: (Track.SubtitleTrack?) -> Unit,
    onDismissRequest: () -> Unit
) {
    val (selectedSubtitleOption, onSubtitleOptionSelected) = remember {
        mutableStateOf(selectedSubtitleTrack)
    }

    val (selectedAudioOption, onAudioOptionSelected) = remember {
        mutableStateOf(audioTracks?.firstOrNull())
    }

    val tabs = mutableListOf(
        subtitlesTab(
            selectedOption = selectedSubtitleOption,
            selectedSubtitleTrack = selectedSubtitleTrack,
            onOptionSelected = onSubtitleOptionSelected,
            subtitleTracks = subtitleTracks ?: emptyList()
        ),
        subtitleSettingsTab()
    )

    TabbedSimpleDialog(
        tabs = tabs,
        onDismissRequest = onDismissRequest,
        opened = opened,
        confirmButton = {
            DefaultDialogConfirmButton(
                enabled = selectedSubtitleOption != selectedSubtitleTrack
            ) {
                onSubtitleTrackSelected(selectedSubtitleOption)
                onDismissRequest()
            }
        },
    )
}