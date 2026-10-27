package com.sf.tadami.ui.tabs.more.settings.screens.advanced

import com.sf.tadami.ui.PlatformUi
import com.sf.tadami.BackgroundJobs
import org.jetbrains.compose.resources.StringResource
import com.sf.tadami.utils.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import org.jetbrains.compose.resources.stringResource
import androidx.navigation.NavHostController
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.navigation.graphs.discover.DiscoverRoutes
import com.sf.tadami.navigation.graphs.settings.AdvancedSettingsRoutes
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.notifications.libraryupdate.LibraryUpdateTarget
import com.sf.tadami.preferences.advanced.AdvancedPreferences
import com.sf.tadami.preferences.model.DataStoreState
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.ui.tabs.more.settings.components.PreferenceScreen
import com.sf.tadami.ui.utils.UiToasts
import kotlinx.coroutines.launch
import okhttp3.Headers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AdvancedPreferencesScreen(
    val navController: NavHostController
) : PreferenceScreen {
    override val title: StringResource = Res.string.preferences_advanced

    override val backHandler: (() -> Unit) = { navController.navigateUp() }

    @Composable
    override fun getPreferences(): List<Preference> {
        val advancedPreferencesState = rememberDataStoreState(AdvancedPreferences)
        val advancedPreferences by advancedPreferencesState.value.collectAsState()
        return listOf(
            getDataGroup(),
            getExtensionsGroup(),
            getNetworkGroup(advancedPreferences, advancedPreferencesState),
        ) + with(Injekt.get<PlatformUi>()) { advancedPreferenceGroups(navController) }
    }

    @Composable
    private fun getExtensionsGroup(): Preference.PreferenceCategory {
        return Preference.PreferenceCategory(
            title = stringResource(Res.string.label_extensions),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(Res.string.ext_repos_title),
                    subtitle = stringResource(Res.string.ext_repos_summary),
                    onClick = { navController.navigate(DiscoverRoutes.EXTENSION_REPOS) },
                ),
            ),
        )
    }

    @Composable
    private fun getDataGroup(): Preference.PreferenceCategory {
        val scope = rememberCoroutineScope()
        val dumpCrashLogs = Injekt.get<PlatformUi>().rememberCrashLogsDump()
        return Preference.PreferenceCategory(
            title = stringResource(Res.string.preferences_advanced_data),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(Res.string.pref_clear_database),
                    subtitle = stringResource(Res.string.pref_clear_database_summary),
                    onClick = {
                        navController.navigate(AdvancedSettingsRoutes.CLEAR_DATABASE)
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(Res.string.action_refresh_library_details),
                    onClick = {
                        val started = Injekt.get<BackgroundJobs>().startLibraryUpdate(target = LibraryUpdateTarget.DETAILS)
                        UiToasts.showToast(if (started) Res.string.refresh_details_starting else Res.string.update_running)
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(Res.string.pref_dump_crash_logs),
                    subtitle = stringResource(Res.string.pref_dump_crash_logs_summary),
                    onClick = {
                        scope.launch { dumpCrashLogs() }
                    },
                ),
            )
        )
    }

    @Composable
    private fun getNetworkGroup(
        advancedPreferences: AdvancedPreferences,
        advancedPreferencesState: DataStoreState<AdvancedPreferences>
    ): Preference.PreferenceCategory {
        val networkHelper = remember { Injekt.get<NetworkHelper>() }
        return Preference.PreferenceCategory(
            title = stringResource(Res.string.category_network),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(Res.string.pref_clear_cookies),
                    onClick = {
                        networkHelper.cookieManager.removeAll()
                        UiToasts.showToast(Res.string.cookies_cleared)
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(Res.string.pref_clear_webview_data),
                    onClick = {
                        try {
                            Injekt.get<PlatformUi>().clearWebViewData()
                            // Deleting app_webview/ while the WebView is loaded would leave it
                            // writing cookies to unlinked files, so clear them through the API
                            networkHelper.cookieManager.removeAll()
                            networkHelper.cookieManager.flush()
                            UiToasts.showToast(Res.string.webview_data_deleted)
                        } catch (e: Throwable) {
                            Log.d("AdvancedNetworkSettings", e.stackTraceToString())
                            UiToasts.showToast(Res.string.cache_delete_error)
                        }
                    },
                ),
                Preference.PreferenceItem.EditTextPreference(
                    value = advancedPreferences.userAgent,
                    title = stringResource(Res.string.pref_user_agent_string),
                    onValueChanged = {
                        try {
                            // OkHttp checks for valid values internally
                            Headers.Builder().add("User-Agent", it)
                        } catch (_: IllegalArgumentException) {
                            UiToasts.showToast(Res.string.error_user_agent_string_invalid)
                            return@EditTextPreference false
                        }
                        advancedPreferencesState.setValue(
                            advancedPreferences.copy(
                                userAgent = it
                            )
                        )
                        true
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(Res.string.pref_reset_user_agent_string),
                    enabled = remember(advancedPreferences.userAgent) { advancedPreferences.userAgent != NetworkHelper.defaultUserAgent },
                    onClick = {
                        // Stored as "follow the WebView version" rather than as a fixed string
                        advancedPreferencesState.setValue(
                            advancedPreferences.copy(
                                userAgent = NetworkHelper.defaultUserAgent
                            )
                        )
                    },
                ),
            )
        )
    }
}