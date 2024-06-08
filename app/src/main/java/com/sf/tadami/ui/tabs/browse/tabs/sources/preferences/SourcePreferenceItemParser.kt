package com.sf.tadami.ui.tabs.browse.tabs.sources.preferences

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.preferences.model.SourcePreference
import com.sf.tadami.ui.tabs.more.settings.widget.EditTextPreferenceWidget
import com.sf.tadami.ui.tabs.more.settings.widget.MultiSelectPreference
import com.sf.tadami.ui.tabs.more.settings.widget.ReorderStringPreference
import com.sf.tadami.ui.tabs.more.settings.widget.SelectPreference
import com.sf.tadami.ui.tabs.more.settings.widget.TogglePreference

@Composable
fun SourcePreferenceItemParser(
    item: SourcePreference.PreferenceItem<*>,
    prefs: Preferences,
    customPrefsVerticalPadding : Dp? =  null,
    onPrefChanged: (key: Preferences.Key<*>, value: Any) -> Unit
) {
    AnimatedVisibility(
        visible = item.enabled,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        content = {
            when (item) {

                is SourcePreference.PreferenceItem.SelectPreference<*> -> {
                    SelectPreference(
                        customPrefsVerticalPadding = customPrefsVerticalPadding,
                        value = prefs[item.key] ?: item.defaultValue,
                        items = item.items,
                        title = item.title,
                        subtitleProvider = item.subtitleProvider,
                        onValueChange = {
                            onPrefChanged(item.key, it)
                            item.castedOnValueChanged(it)
                        }
                    )
                }

                is SourcePreference.PreferenceItem.MultiSelectPreference -> {
                    MultiSelectPreference(
                        customPrefsVerticalPadding = customPrefsVerticalPadding,
                        value = prefs[item.key] ?: item.defaultValue,
                        items = item.items,
                        title = item.title,
                        overrideOkButton = item.overrideOkButton,
                        subtitleProvider = {
                            ""
                        },
                        onValueChange = {
                            onPrefChanged(item.key, it)
                            item.onValueChanged(it)
                        }
                    )
                }

                is SourcePreference.PreferenceItem.TogglePreference -> {
                    TogglePreference(
                        customPrefsVerticalPadding = customPrefsVerticalPadding,
                        title = item.title,
                        subtitle = item.subtitle,
                        icon = item.icon,
                        checked = prefs[item.key] ?: item.defaultValue,
                        onCheckedChanged = {
                            onPrefChanged(item.key, it)
                            item.onValueChanged(it)
                        }
                    )
                }

                is SourcePreference.PreferenceItem.EditTextPreference -> {
                    EditTextPreferenceWidget(
                        customPrefsVerticalPadding = customPrefsVerticalPadding,
                        title = item.title,
                        subtitle = item.subtitle,
                        icon = item.icon,
                        value = prefs[item.key] ?: item.defaultValue,
                        defaultValue = item.defaultValue,
                        onConfirm = {
                            onPrefChanged(item.key, it)
                            val accepted = item.onValueChanged(it)
                            accepted
                        },
                    )
                }

                is SourcePreference.PreferenceItem.ReorderStringPreference -> {
                    ReorderStringPreference(
                        customPrefsVerticalPadding = customPrefsVerticalPadding,
                        valueList = (prefs[item.key] ?: item.defaultValue).split(","),
                        items = item.items,
                        title = item.title,
                        subtitleProvider = {
                            item.subtitle
                        },
                        onValueChange = {
                            onPrefChanged(item.key, it)
                            item.onValueChanged(it)
                        }
                    )
                }
            }
        },
    )

}