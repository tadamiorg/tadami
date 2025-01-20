package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.videoselection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.videoselection.tabs.videoTab
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogCancelButton
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton
import com.sf.tadami.ui.components.dialog.simple.TabbedSimpleDialog

@Composable
fun VideoSelectionDialog(
    opened: Boolean,
    sources: List<StreamSource>,
    selectedSource: StreamSource? = null,
    onSelectSource: (source: StreamSource) -> Unit,
    onDismissRequest: () -> Unit
) {
    val (selectedOption, onOptionSelected) = remember {
        mutableStateOf(selectedSource ?: sources.firstOrNull())
    }

    val tabs = mutableListOf(
        videoTab(
            sources = sources,
            selectedSource = selectedSource,
            selectedOption = selectedOption,
            onOptionSelected = onOptionSelected
        )
    )

    TabbedSimpleDialog(
        tabs = tabs,
        onDismissRequest = onDismissRequest,
        opened = opened,
        confirmButton = {
            DefaultDialogConfirmButton(
                enabled = selectedOption != selectedSource
            ) {
                if (selectedOption != null) {
                    onSelectSource(selectedOption)
                    onDismissRequest()
                }
            }
        },
        dismissButton = {
            DefaultDialogCancelButton()
        }
    )
}