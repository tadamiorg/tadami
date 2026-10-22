package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.R
import com.sf.tadami.preferences.model.SourcePreference
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.settings.tabs.ApplicationTab
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.settings.tabs.SourceTab
import com.sf.tadami.ui.components.modal.PlayerPanel

/** The player's settings, and the source's when it has some. */
@Composable
fun SettingsDialog(
    opened: Boolean,
    sourceDatastore: DataStore<Preferences>?,
    sourcePrefsitems: List<SourcePreference> = emptyList(),
    onDismissRequest: () -> Unit
) {
    val tabs = buildList {
        add(stringResource(R.string.notification_app_group))
        if (sourceDatastore != null) add(stringResource(R.string.label_source))
    }
    PlayerPanel(
        visible = opened,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.label_settings),
        tabs = tabs,
    ) { page ->
        if (page == 0 || sourceDatastore == null) {
            ApplicationTab()
        } else {
            SourceTab(
                sourceDatastore = sourceDatastore,
                items = sourcePrefsitems.filter { pref ->
                    if (pref is SourcePreference.PreferenceCategory) {
                        pref.videoCategory
                    } else true
                }
            )
        }
    }
}
