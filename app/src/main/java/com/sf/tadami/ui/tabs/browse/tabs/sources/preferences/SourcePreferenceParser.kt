package com.sf.tadami.ui.tabs.browse.tabs.sources.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.datastore.preferences.core.Preferences

import com.sf.tadami.preferences.model.SourcePreference
import com.sf.tadami.ui.tabs.more.settings.widget.PreferenceCategory

@Composable
fun SourcePreferenceParser(
    modifier: Modifier = Modifier,
    customPrefsVerticalPadding : Dp? = null,
    items: List<SourcePreference>,
    prefs : Preferences,
    onPrefChanged : (key : Preferences.Key<*>,value : Any) -> Unit
) {

    LazyColumn(
        modifier = modifier
    ) {
        items.fastForEach { preference ->
            when (preference) {
                is SourcePreference.PreferenceCategory -> {
                    item {
                        Column {
                            PreferenceCategory(title = preference.title)
                        }
                    }
                    items(preference.preferenceItems) { categoryPreference->
                        SourcePreferenceItemParser(item = categoryPreference, prefs = prefs,onPrefChanged = onPrefChanged, customPrefsVerticalPadding = customPrefsVerticalPadding)
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                }
                is SourcePreference.PreferenceItem<*> -> item {
                    SourcePreferenceItemParser(item = preference, prefs = prefs,onPrefChanged = onPrefChanged,customPrefsVerticalPadding = customPrefsVerticalPadding)
                }
            }
        }
    }
}