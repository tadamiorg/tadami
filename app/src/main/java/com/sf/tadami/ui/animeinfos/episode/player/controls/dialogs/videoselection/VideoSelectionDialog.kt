package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.videoselection

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.videoselection.tabs.VideoTab
import com.sf.tadami.ui.components.modal.ModalRules
import com.sf.tadami.ui.components.modal.PlayerPanel

/** The streams of the episode: a tap switches to it and closes the panel. */
@Composable
fun VideoSelectionDialog(
    opened: Boolean,
    sources: List<StreamSource>,
    selectedSource: StreamSource? = null,
    onSelectSource: (source: StreamSource) -> Unit,
    onDismissRequest: () -> Unit
) {
    PlayerPanel(
        visible = opened,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.label_video),
    ) {
        VideoTab(
            sources = sources,
            selectedSource = selectedSource ?: sources.firstOrNull(),
            onSelect = {
                if (ModalRules.singleChoiceApplies(selectedSource, it)) onSelectSource(it)
                onDismissRequest()
            },
        )
    }
}
