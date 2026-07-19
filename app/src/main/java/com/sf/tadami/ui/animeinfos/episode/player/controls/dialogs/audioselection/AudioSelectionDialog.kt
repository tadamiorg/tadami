package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.audioselection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.sf.tadami.source.model.Track
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.audioselection.tabs.audioTab
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogCancelButton
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton
import com.sf.tadami.ui.components.dialog.simple.TabbedSimpleDialog

/** Standalone audio-language picker — same shape as the video-source selection dialog. */
@Composable
fun AudioSelectionDialog(
    opened: Boolean,
    audioTracks: List<Track.AudioTrack>,
    selectedAudioTrack: Track.AudioTrack? = null,
    onAudioTrackSelected: (Track.AudioTrack) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val (selectedOption, onOptionSelected) = remember {
        mutableStateOf(selectedAudioTrack ?: audioTracks.firstOrNull())
    }

    val tabs = mutableListOf(
        audioTab(
            audioTracks = audioTracks,
            selectedAudioTrack = selectedAudioTrack,
            selectedOption = selectedOption,
            onOptionSelected = onOptionSelected,
        )
    )

    TabbedSimpleDialog(
        tabs = tabs,
        onDismissRequest = onDismissRequest,
        opened = opened,
        confirmButton = {
            DefaultDialogConfirmButton(
                enabled = selectedOption != selectedAudioTrack
            ) {
                if (selectedOption != null) {
                    onAudioTrackSelected(selectedOption)
                    onDismissRequest()
                }
            }
        },
        dismissButton = {
            DefaultDialogCancelButton()
        }
    )
}
