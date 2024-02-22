package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.settings

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.preferences.model.SourcePreference
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.settings.tabs.applicationTab
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.settings.tabs.sourceTab
import com.sf.tadami.ui.components.dialog.simple.TabbedSimpleDialog

@Composable
fun SettingsDialog(
    opened: Boolean,
    sourceDatastore: DataStore<Preferences>?,
    sourcePrefsitems: List<SourcePreference> = emptyList(),
    onDismissRequest: () -> Unit
) {
    val tabs = mutableListOf(
        applicationTab()
    )
    if (sourceDatastore != null) tabs.add(
        sourceTab(
            sourceDatastore = sourceDatastore,
            items = sourcePrefsitems.filter { pref ->
                if (pref is SourcePreference.PreferenceCategory) {
                    pref.videoCategory
                } else true
            }
        )
    )
    TabbedSimpleDialog(
        tabs = tabs,
        onDismissRequest = onDismissRequest,
        opened = opened
    )
}