package com.sf.tadami.ui.tabs.more.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.navigation.NavHostController
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.navigation.graphs.settings.AdvancedSettingsRoutes
import com.sf.tadami.navigation.graphs.settings.DataSettingsRoutes
import com.sf.tadami.navigation.graphs.settings.SettingsRoutes
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.topappbar.TadaTopAppBar
import com.sf.tadami.ui.components.widgets.TwoPanelBox
import com.sf.tadami.ui.tabs.more.settings.components.LocalSettingsEndPane
import com.sf.tadami.ui.tabs.more.settings.search.SearchableSettings
import com.sf.tadami.ui.utils.isTabletUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
) {
    val twoPane = isTabletUi()
    // The category on the end pane of the two-pane settings, the first one at first
    var selectedRoute by rememberSaveable { mutableStateOf(SettingsRoutes.APPEARANCE) }
    val openCategory: (String) -> Unit = { route ->
        if (twoPane) selectedRoute = route else navController.navigate(route)
    }
    val categories = listOf(
        SettingsRoutes.APPEARANCE to Preference.PreferenceItem.TextPreference(
            title = stringResource(Res.string.label_appearance),
            subtitle = stringResource(Res.string.pref_appearance_summary),
            icon = Icons.Outlined.Palette,
            onClick = {
                openCategory(SettingsRoutes.APPEARANCE)
            }
        ),
        SettingsRoutes.LIBRARY to Preference.PreferenceItem.TextPreference(
            title = stringResource(Res.string.settings_tab_library_preferences_title),
            subtitle = stringResource(Res.string.settings_tab_library_preferences_subtitle),
            icon = Icons.Outlined.VideoLibrary,
            onClick = {
                openCategory(SettingsRoutes.LIBRARY)
            }
        ),
        SettingsRoutes.PLAYER to Preference.PreferenceItem.TextPreference(
            title = stringResource(Res.string.settings_tab_library_player_preferences_title),
            subtitle = stringResource(Res.string.settings_tab_library_player_preferences_subtitle),
            icon = Icons.Outlined.OndemandVideo,
            onClick = {
                openCategory(SettingsRoutes.PLAYER)
            }
        ),
        SettingsRoutes.DOWNLOADS to Preference.PreferenceItem.TextPreference(
            title = stringResource(Res.string.preferences_download_title),
            subtitle = stringResource(Res.string.preferences_download_summary),
            icon = Icons.Outlined.Download,
            onClick = {
                openCategory(SettingsRoutes.DOWNLOADS)
            }
        ),
        DataSettingsRoutes.HOME to Preference.PreferenceItem.TextPreference(
            title = stringResource(Res.string.settings_tab_library_data_preferences_title),
            subtitle = stringResource(Res.string.settings_tab_library_backup_preferences_subtitle),
            icon = Icons.Outlined.Backup,
            onClick = {
                openCategory(DataSettingsRoutes.HOME)
            }
        ),
        AdvancedSettingsRoutes.HOME to Preference.PreferenceItem.TextPreference(
            title = stringResource(Res.string.preferences_advanced),
            subtitle = stringResource(Res.string.preferences_advanced_subtitle),
            icon = Icons.Outlined.Code,
            onClick = {
                openCategory(AdvancedSettingsRoutes.HOME)
            }
        )
    )

    val categoriesList = @Composable { listModifier: Modifier ->
        SettingsCategories(
            modifier = listModifier,
            navController = navController,
            categories = categories.map { it.second },
            selectedIndex = categories.indexOfFirst { it.first == selectedRoute }.takeIf { twoPane },
        )
    }

    if (!twoPane) {
        categoriesList(modifier)
    } else {
        TwoPanelBox(
            modifier = modifier,
            startContent = { categoriesList(Modifier) },
            endContent = {
                // A new screen and its state for each category, as when opened by its route
                key(selectedRoute) {
                    val screen = remember { SearchableSettings.screens.first { it.route == selectedRoute }.create(navController) }
                    CompositionLocalProvider(LocalSettingsEndPane provides true) {
                        screen.Content()
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsCategories(
    modifier: Modifier,
    navController: NavHostController,
    categories: List<Preference.PreferenceItem.TextPreference>,
    selectedIndex: Int?,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TadaTopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.label_settings),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                actions = listOf(
                    Action.Vector(
                        title = Res.string.action_search_settings,
                        icon = Icons.Outlined.Search,
                        onClick = { navController.navigate(SettingsRoutes.SEARCH) }
                    )
                )
            )
        }
    ) {
        SettingsComponent(
            modifier = Modifier.padding(it),
            preferences = categories,
            selectedIndex = selectedIndex,
        )
    }
}