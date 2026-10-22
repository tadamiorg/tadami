package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.episodetooltip

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.R
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
        title = stringResource(R.string.player_epsiode_tooltip_title),
    ) {
        if (sourceDatastore != null) {
            TooltipTab(
                sourceDatastore = sourceDatastore,
                tooltipContent = tooltipContent,
            )
        }
    }
}
