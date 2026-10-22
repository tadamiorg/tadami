package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.tracksselection

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.source.model.Track
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.tracksselection.tabs.SubtitleSettingsTab
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.tracksselection.tabs.SubtitlesTab
import com.sf.tadami.ui.components.modal.ModalRules
import com.sf.tadami.ui.components.modal.PlayerPanel

/**
 * The subtitle tracks, a tap switches to it and closes the panel, and their appearance, changed live
 * over the video: no scrim on that page.
 */
@Composable
fun TracksSelectionDialog(
    opened: Boolean,
    subtitleTracks: List<Track.SubtitleTrack>? = null,
    selectedSubtitleTrack: Track.SubtitleTrack? = null,
    onSubtitleTrackSelected: (Track.SubtitleTrack?) -> Unit,
    onDismissRequest: () -> Unit
) {
    PlayerPanel(
        visible = opened,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.label_subtitles),
        tabs = listOf(stringResource(R.string.label_subtitles), stringResource(R.string.label_settings)),
        clearScrim = { page -> page == 1 },
    ) { page ->
        if (page == 0) {
            SubtitlesTab(
                subtitleTracks = subtitleTracks ?: emptyList(),
                selectedSubtitleTrack = selectedSubtitleTrack,
                onSelect = {
                    if (ModalRules.singleChoiceApplies(selectedSubtitleTrack, it)) onSubtitleTrackSelected(it)
                    onDismissRequest()
                },
            )
        } else {
            SubtitleSettingsTab()
        }
    }
}
