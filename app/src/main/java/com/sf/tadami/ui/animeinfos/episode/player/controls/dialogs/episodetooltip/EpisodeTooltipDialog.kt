package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.episodetooltip

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton
import com.sf.tadami.ui.components.dialog.simple.TabbedSimpleDialog
import com.sf.tadami.ui.components.screens.ScreenTabContent

@Composable
fun EpisodeTooltipDialog(
    opened: Boolean,
    sourceDatastore: DataStore<Preferences>?,
    tooltipContent: String,
    onDismissRequest: () -> Unit
) {
    val tabs = mutableListOf<ScreenTabContent>()

    if (sourceDatastore != null) tabs.add(
        tooltipTab(
            sourceDatastore = sourceDatastore,
            tooltipContent = tooltipContent,
        )
    )

    TabbedSimpleDialog(
        tabs = tabs,
        onDismissRequest = onDismissRequest,
        opened = opened,
        confirmButton = {
            DefaultDialogConfirmButton{
                onDismissRequest()
            }
        }
    )
}