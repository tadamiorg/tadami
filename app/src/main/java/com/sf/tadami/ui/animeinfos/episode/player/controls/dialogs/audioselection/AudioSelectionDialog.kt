package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.audioselection

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.source.model.Track
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.audioselection.tabs.AudioTab
import com.sf.tadami.ui.components.modal.ModalRules
import com.sf.tadami.ui.components.modal.PlayerPanel

/** Standalone audio-language picker — same shape as the video-source selection panel. */
@Composable
fun AudioSelectionDialog(
    opened: Boolean,
    audioTracks: List<Track.AudioTrack>,
    selectedAudioTrack: Track.AudioTrack? = null,
    onAudioTrackSelected: (Track.AudioTrack) -> Unit,
    onDismissRequest: () -> Unit,
) {
    PlayerPanel(
        visible = opened,
        onDismissRequest = onDismissRequest,
        title = stringResource(Res.string.label_audio),
    ) {
        AudioTab(
            audioTracks = audioTracks,
            selectedAudioTrack = selectedAudioTrack ?: audioTracks.firstOrNull(),
            onSelect = {
                if (ModalRules.singleChoiceApplies(selectedAudioTrack, it)) onAudioTrackSelected(it)
                onDismissRequest()
            },
        )
    }
}
