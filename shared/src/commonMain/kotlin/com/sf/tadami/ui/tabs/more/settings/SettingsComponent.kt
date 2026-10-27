package com.sf.tadami.ui.tabs.more.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.ui.tabs.more.settings.widget.TextPreference

@Composable
fun SettingsComponent(
    modifier: Modifier = Modifier,
    preferences : List<Preference.PreferenceItem.TextPreference>,
    // The two-pane settings: the category shown on the end pane, highlighted as Mihon
    selectedIndex : Int? = null,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
    ) {
        itemsIndexed(preferences){ index, pref ->
            val selected = index == selectedIndex
            CompositionLocalProvider(
                LocalContentColor provides if (selected) MaterialTheme.colorScheme.onSurfaceVariant else LocalContentColor.current
            ) {
                TextPreference(
                    modifier = if (selectedIndex == null) Modifier else Modifier
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .then(if (selected) Modifier.background(MaterialTheme.colorScheme.surfaceVariant) else Modifier),
                    title = pref.title,
                    subtitle = pref.subtitle,
                    icon = pref.icon,
                    onPreferenceClick = pref.onClick
                )
            }
        }
    }
}