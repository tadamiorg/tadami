package com.sf.tadami.ui.tabs.more.settings.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.settings.SettingsRoutes
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.ui.components.grid.EmptyScreen
import com.sf.tadami.ui.components.topappbar.search.SearchBarExpanded
import com.sf.tadami.ui.tabs.more.settings.components.PreferenceScreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsSearchScreen(navController: NavHostController) {
    var query by rememberSaveable { mutableStateOf("") }
    val screens = remember(navController) {
        SearchableSettings.screens.map { it.route to it.create(navController) }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    DisposableEffect(Unit) {
        onDispose { keyboardController?.hide() }
    }

    Scaffold(
        topBar = {
            SearchBarExpanded(
                value = query,
                placeholder = stringResource(R.string.action_search_settings),
                onSearchChange = { query = it },
                onSearch = {},
                onSearchCancel = { navController.navigateUp() },
            )
        }
    ) { contentPadding ->
        val index = settingsIndex(screens)
        // The query is read out of composition, so typing doesn't build the index again
        val results by produceState<List<SettingsSearchEntry>?>(initialValue = null, index) {
            snapshotFlow { query }.collectLatest {
                value = if (it.isBlank()) null else SettingsSearch.search(index, it)
            }
        }

        val shown = results
        when {
            shown == null -> EmptyScreen(
                modifier = Modifier.padding(contentPadding),
                message = stringResource(R.string.settings_search_hint)
            )

            shown.isEmpty() -> EmptyScreen(
                modifier = Modifier.padding(contentPadding),
                message = stringResource(R.string.pager_no_results)
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            ) {
                items(shown) { entry ->
                    SearchResult(entry) {
                        PreferenceScreen.highlightKey = entry.title
                        // In place of the search, as back leads to the settings
                        navController.navigate(entry.route) {
                            popUpTo(SettingsRoutes.SEARCH) { inclusive = true }
                        }
                    }
                }
            }
        }
    }
}

/** The entries of every searchable screen, from the preferences the screens show */
@Composable
private fun settingsIndex(screens: List<Pair<String, PreferenceScreen>>): List<SettingsSearchEntry> {
    return screens.flatMap { (route, screen) ->
        val screenTitle = screen.getTitle()
        screen.getPreferences().searchablePreferences().map { (group, preference) ->
            SettingsSearchEntry(
                route = route,
                screenTitle = screenTitle,
                group = group,
                title = preference.title,
                summary = (preference as? Preference.PreferenceItem<*>)?.let { summary(it) },
            )
        }
    }
}

/** The summary the preference shows */
@Composable
private fun summary(item: Preference.PreferenceItem<*>): String? {
    val summary = when (item) {
        is Preference.PreferenceItem.SelectPreference<*> -> item.subtitleProvider()
        is Preference.PreferenceItem.MultiSelectPreference -> item.subtitleProvider()
        is Preference.PreferenceItem.MultiSelectPreferenceInt -> item.subtitleProvider()
        is Preference.PreferenceItem.EditTextPreference -> item.subtitle?.format(item.value)
        else -> item.subtitle
    }
    return summary?.takeIf { it.isNotBlank() }
}

@Composable
private fun SearchResult(entry: SettingsSearchEntry, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = entry.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (entry.summary != null) {
            Text(
                text = entry.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = entry.breadcrumb,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
