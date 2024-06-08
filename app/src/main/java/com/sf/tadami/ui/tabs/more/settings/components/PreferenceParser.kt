package com.sf.tadami.ui.tabs.more.settings.components

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
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.ui.tabs.more.settings.widget.PreferenceCategory

@Composable
fun PreferenceParser(
    modifier: Modifier = Modifier,
    customPrefsVerticalPadding : Dp? = null,
    items: List<Preference>
) {
    LazyColumn(
        modifier = modifier
    ) {
        items.fastForEach { preference ->
            when (preference) {
                is Preference.PreferenceCategory -> {
                    item {
                        Column {
                            PreferenceCategory(title = preference.title)
                        }
                    }
                    items(preference.preferenceItems) { categoryPreference->
                        PreferenceItemParser(item = categoryPreference,customPrefsVerticalPadding = customPrefsVerticalPadding)
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                }
                is Preference.PreferenceItem<*> -> item {
                    PreferenceItemParser(item = preference,customPrefsVerticalPadding = customPrefsVerticalPadding)
                }
            }
        }
    }
}