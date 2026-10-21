package com.sf.tadami.ui.tabs.more.settings.search

import androidx.navigation.NavHostController
import com.sf.tadami.navigation.graphs.settings.AdvancedSettingsRoutes
import com.sf.tadami.navigation.graphs.settings.DataSettingsRoutes
import com.sf.tadami.navigation.graphs.settings.SettingsRoutes
import com.sf.tadami.ui.tabs.more.settings.components.PreferenceScreen
import com.sf.tadami.ui.tabs.more.settings.screens.advanced.AdvancedPreferencesScreen
import com.sf.tadami.ui.tabs.more.settings.screens.advanced.background.WorkerInfosScreen
import com.sf.tadami.ui.tabs.more.settings.screens.advanced.data.ClearDatabaseScreen
import com.sf.tadami.ui.tabs.more.settings.screens.appearance.AppearancePreferencesScreen
import com.sf.tadami.ui.tabs.more.settings.screens.data.DataPreferencesScreen
import com.sf.tadami.ui.tabs.more.settings.screens.data.backup.CreateBackupScreen
import com.sf.tadami.ui.tabs.more.settings.screens.data.backup.RestoreBackupScreen
import com.sf.tadami.ui.tabs.more.settings.screens.download.DownloadPreferencesScreen
import com.sf.tadami.ui.tabs.more.settings.screens.library.LibraryPreferencesScreen
import com.sf.tadami.ui.tabs.more.settings.screens.player.PlayerPreferencesScreen
import kotlin.reflect.KClass

/**
 * The settings screens the settings search indexes, from the preferences they show. Every
 * [PreferenceScreen] is either here or in [notSearchable], which SearchableSettingsTest checks.
 */
object SearchableSettings {

    class Screen(
        val route: String,
        val type: KClass<out PreferenceScreen>,
        val create: (NavHostController) -> PreferenceScreen,
    )

    private inline fun <reified T : PreferenceScreen> screen(route: String, noinline create: (NavHostController) -> T) =
        Screen(route, T::class, create)

    /** In the order of the settings screen */
    val screens: List<Screen> = listOf(
        screen(SettingsRoutes.APPEARANCE, ::AppearancePreferencesScreen),
        screen(SettingsRoutes.LIBRARY, ::LibraryPreferencesScreen),
        screen(SettingsRoutes.PLAYER, ::PlayerPreferencesScreen),
        screen(SettingsRoutes.DOWNLOADS, ::DownloadPreferencesScreen),
        screen(DataSettingsRoutes.HOME, ::DataPreferencesScreen),
        screen(AdvancedSettingsRoutes.HOME, ::AdvancedPreferencesScreen),
    )

    /** Tools rather than settings, each opened by a setting of a searchable screen, which the search finds */
    val notSearchable: Set<KClass<out PreferenceScreen>> = setOf(
        WorkerInfosScreen::class,
        ClearDatabaseScreen::class,
        CreateBackupScreen::class,
        // Opened with the backup picked, for that backup
        RestoreBackupScreen::class,
    )
}
