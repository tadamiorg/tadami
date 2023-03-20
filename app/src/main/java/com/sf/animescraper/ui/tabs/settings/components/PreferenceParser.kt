package com.sf.animescraper.ui.tabs.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.sf.animescraper.ui.tabs.settings.model.Preference
import com.sf.animescraper.ui.tabs.settings.widget.PreferenceCategory

@Composable
fun PreferenceParser(
    modifier: Modifier = Modifier,
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
                        PreferenceItemParser(item = categoryPreference)
                    }
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                }
                is Preference.PreferenceItem<*> -> item {
                    PreferenceItemParser(item = preference)
                }
            }
        }
    }
}