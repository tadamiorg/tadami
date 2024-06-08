package com.sf.tadami.ui.tabs.more.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.ui.tabs.more.settings.widget.TextPreference

@Composable
fun SettingsComponent(
    modifier: Modifier = Modifier,
    preferences : List<Preference.PreferenceItem.TextPreference>
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
    ) {
        items(preferences){ pref ->
            TextPreference(
                title = pref.title,
                subtitle = pref.subtitle,
                icon = pref.icon,
                onPreferenceClick = pref.onClick
            )
        }
    }
}