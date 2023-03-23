package com.sf.tadami.ui.tabs.settings.components

import androidx.compose.runtime.Composable
import com.sf.tadami.ui.tabs.settings.model.Preference
import com.sf.tadami.ui.tabs.settings.widget.MultiSelectPreference
import com.sf.tadami.ui.tabs.settings.widget.SelectPreference
import com.sf.tadami.ui.tabs.settings.widget.TextPreference
import com.sf.tadami.ui.tabs.settings.widget.TogglePreference

@Composable
fun PreferenceItemParser(
    item: Preference.PreferenceItem<*>
) {
    when(item){
        is Preference.PreferenceItem.TextPreference -> {
            TextPreference(
                title = item.title,
                subtitle = item.subtitle,
                icon = item.icon,
                onPreferenceClick = item.onClick
            )
        }
        is Preference.PreferenceItem.SelectPreference<*> -> {
            SelectPreference(
                value = item.value,
                items = item.items,
                title = item.title,
                subtitleProvider = item.subtitleProvider,
                onValueChange = {
                    item.castedOnValueChanged(it)
                }
            )
        }
        is Preference.PreferenceItem.MultiSelectPreference -> {
            MultiSelectPreference(
                value = item.value,
                items = item.items,
                title = item.title,
                subtitleProvider = item.subtitleProvider,
                onValueChange = {
                    item.onValueChanged(it)
                }
            )
        }
        is Preference.PreferenceItem.TogglePreference -> {
            TogglePreference(
                title = item.title,
                subtitle = item.subtitle,
                icon = item.icon,
                checked = item.value,
                onCheckedChanged = {
                    item.onValueChanged(it)
                }
            )
        }
    }
}