package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.settings.tabs

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.R
import com.sf.tadami.preferences.model.SourcePreference
import com.sf.tadami.preferences.model.rememberUnknownDataStoreState
import com.sf.tadami.ui.components.screens.ScreenTabContent
import com.sf.tadami.ui.tabs.browse.tabs.sources.preferences.SourcePreferenceParser

@Suppress("UNCHECKED_CAST")
@Composable()
fun sourceTab(
    sourceDatastore : DataStore<Preferences>,
    items : List<SourcePreference> = emptyList()
) : ScreenTabContent {
    val datastoreState = rememberUnknownDataStoreState(unknownDataStore = sourceDatastore)
    val sourcePreferences by datastoreState.value.collectAsState()
    return ScreenTabContent(
        titleRes = R.string.label_source,
    ){ contentPadding: PaddingValues, _ ->
        SourcePreferenceParser(
            customPrefsVerticalPadding = 8.dp,
            modifier = Modifier.padding(contentPadding),
            items = items,
            prefs = sourcePreferences,
            onPrefChanged = { key, value ->
                datastoreState.setValue(value,key as Preferences.Key<Any>)
            }
        )
    }
}