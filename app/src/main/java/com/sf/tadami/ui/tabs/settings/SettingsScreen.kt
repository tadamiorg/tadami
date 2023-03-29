package com.sf.tadami.ui.tabs.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.sf.tadami.navigation.graphs.SettingsRoutes
import com.sf.tadami.ui.tabs.settings.model.Preference
import com.sf.tadami.R

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