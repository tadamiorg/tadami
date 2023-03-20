package com.sf.animescraper.ui.tabs.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.navigation.NavHostController
import com.sf.animescraper.R
import com.sf.animescraper.navigation.graphs.SettingsRoutes
import com.sf.animescraper.ui.tabs.settings.model.Preference
import kotlinx.coroutines.flow.map
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_tab_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            )
        }
    ) {
        SettingsComponent(
            modifier = Modifier.padding(it),
            preferences = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(id = R.string.settings_tab_library_preferences_title),
                    subtitle = stringResource(id = R.string.settings_tab_library_preferences_subtitle),
                    icon = Icons.Outlined.VideoLibrary,
                    onClick = {
                        navController.navigate(SettingsRoutes.LIBRARY)
                    }
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(id = R.string.settings_tab_library_backup_preferences_title),
                    subtitle = stringResource(id = R.string.settings_tab_library_backup_preferences_subtitle),
                    icon = Icons.Outlined.Backup,
                    onClick = {

                    }
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(id = R.string.settings_tab_library_player_preferences_title),
                    subtitle = stringResource(id = R.string.settings_tab_library_player_preferences_subtitle),
                    icon = Icons.Outlined.OndemandVideo,
                    onClick = {

                    }
                )
            )
        )
    }


}