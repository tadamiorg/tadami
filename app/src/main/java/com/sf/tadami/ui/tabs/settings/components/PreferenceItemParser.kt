package com.sf.tadami.ui.tabs.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.ui.tabs.settings.widget.EditTextPreferenceWidget
import com.sf.tadami.ui.tabs.settings.widget.MultiSelectPreference
import com.sf.tadami.ui.tabs.settings.widget.SelectPreference
import com.sf.tadami.ui.tabs.settings.widget.TextPreference
import com.sf.tadami.ui.tabs.settings.widget.TogglePreference

@Composable
fun PreferenceItemParser(
    customPrefsVerticalPadding : Dp? = null,
    item: Preference.PreferenceItem<*>
) {
    AnimatedVisibility(
        visible = item.enabled,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        content = {
            when(item){
                is Preference.PreferenceItem.TextPreference -> {
                    TextPreference(
                        customPrefsVerticalPadding = customPrefsVerticalPadding,
                        title = item.title,
                        subtitle = item.subtitle,
                        icon = item.icon,
                        onPreferenceClick = item.onClick
                    )
                }
                is Preference.PreferenceItem.SelectPreference<*> -> {
                    SelectPreference(
                        customPrefsVerticalPadding = customPrefsVerticalPadding,
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
                        customPrefsVerticalPadding = customPrefsVerticalPadding,
                        value = item.value,
                        items = item.items,
                        title = item.title,
                        overrideOkButton = item.overrideOkButton,
                        subtitleProvider = item.subtitleProvider,
                        onValueChange = {
                            item.onValueChanged(it)
                        }
                    )
                }
                is Preference.PreferenceItem.MultiSelectPreferenceInt -> {
                    MultiSelectPreference(
                        customPrefsVerticalPadding = customPrefsVerticalPadding,
                        value = item.value,
                        items = item.items,
                        title = item.title,
                        overrideOkButton = item.overrideOkButton,
                        subtitleProvider = item.subtitleProvider,
                        onValueChange = {
                            item.onValueChanged(it.toSortedSet())
                        }
                    )
                }
                is Preference.PreferenceItem.TogglePreference -> {
                    TogglePreference(
                        customPrefsVerticalPadding = customPrefsVerticalPadding,
                        title = item.title,
                        subtitle = item.subtitle,
                        icon = item.icon,
                        checked = item.value,
                        onCheckedChanged = {
                            item.onValueChanged(it)
                        }
                    )
                }
                is Preference.PreferenceItem.EditTextPreference -> {
                    EditTextPreferenceWidget(
                        customPrefsVerticalPadding = customPrefsVerticalPadding,
                        title = item.title,
                        subtitle = item.subtitle,
                        icon = item.icon,
                        value = item.value,
                        defaultValue = item.defaultValue,
                        onConfirm = {
                            val accepted = item.onValueChanged(it)
                            accepted
                        },
                    )
                }
                is Preference.PreferenceItem.CustomPreference -> {
                    item.content(item)
                }
            }
        },
    )

}