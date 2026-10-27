package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.episodetooltip

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.ui.components.modal.PlayerPanel

@Composable
fun EpisodeTooltipDialog(
    opened: Boolean,
    sourceDatastore: DataStore<Preferences>?,
    tooltipContent: String,
    onDismissRequest: () -> Unit
) {
    PlayerPanel(
        visible = opened,
        onDismissRequest = onDismissRequest,
        title = stringResource(Res.string.player_epsiode_tooltip_title),
    ) {
        if (sourceDatastore != null) {
            TooltipTab(
                sourceDatastore = sourceDatastore,
                tooltipContent = tooltipContent,
            )
        }
    }
}
