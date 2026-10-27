package com.sf.tadami.ui.tabs.more.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.ui.tabs.more.settings.search.highlightBackground
import com.sf.tadami.ui.tabs.more.settings.widget.PreferenceCategory
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun PreferenceParser(
    modifier: Modifier = Modifier,
    customPrefsVerticalPadding : Dp? = null,
    items: List<Preference>,
    // The title of the preference to scroll to and highlight
    highlightKey: String? = null
) {
    val listState = rememberLazyListState()
    var highlighted by remember { mutableStateOf(highlightKey) }
    val currentItems by rememberUpdatedState(items)
    LaunchedEffect(Unit) {
        val key = highlighted ?: return@LaunchedEffect
        // Once the screen has settled, as its preferences may load after its first frame
        delay(0.5.seconds)
        val row = currentItems.rowTitles().indexOf(key)
        if (row >= 0) listState.animateScrollToItem(row)
        delay(HIGHLIGHT_DURATION)
        // Not again when scrolling back to it
        highlighted = null
    }

    LazyColumn(
        modifier = modifier,
        state = listState
    ) {
        items.fastForEach { preference ->
            when (preference) {
                is Preference.PreferenceCategory -> {
                    item {
                        Column(modifier = Modifier.highlightBackground(preference.title == highlighted)) {
                            PreferenceCategory(title = preference.title)
                        }
                    }
                    items(preference.preferenceItems) { categoryPreference->
                        PreferenceItemParser(item = categoryPreference,customPrefsVerticalPadding = customPrefsVerticalPadding, highlighted = categoryPreference.title == highlighted)
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                }
                is Preference.PreferenceItem<*> -> item {
                    PreferenceItemParser(item = preference,customPrefsVerticalPadding = customPrefsVerticalPadding, highlighted = preference.title == highlighted)
                }
            }
        }
    }
}

private val HIGHLIGHT_DURATION = 4.seconds

/** The title of each row of the list, in its order: a group's heading, its items, then its spacer */
private fun List<Preference>.rowTitles(): List<String?> = flatMap { preference ->
    when (preference) {
        is Preference.PreferenceCategory -> listOf(preference.title) + preference.preferenceItems.map { it.title } + null
        is Preference.PreferenceItem<*> -> listOf(preference.title)
    }
}