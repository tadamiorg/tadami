package com.sf.tadami.ui.tabs.more.settings.screens.advanced

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.settings.AdvancedSettingsRoutes
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.network.utils.setDefaultSettings
import com.sf.tadami.preferences.advanced.AdvancedPreferences
import com.sf.tadami.preferences.model.DataStoreState
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.ui.tabs.more.settings.components.PreferenceScreen
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.utils.powerManager
import okhttp3.Headers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

class AdvancedPreferencesScreen(
    val navController: NavHostController
) : PreferenceScreen {
    override val title: Int = R.string.preferences_advanced

    override val backHandler: (() -> Unit) = { navController.navigateUp() }

    @Composable
    override fun getPreferences(): List<Preference> {
        val advancedPreferencesState = rememberDataStoreState(AdvancedPreferences)
        val advancedPreferences by advancedPreferencesState.value.collectAsState()
        return listOf(
            getDataGroup(),
            getNetworkGroup(advancedPreferences, advancedPreferencesState),
            getBackgroundJobGroup()
        )
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    @Composable
    private fun getDataGroup(): Preference.PreferenceCategory {
        return Preference.PreferenceCategory(
            title = stringResource(id = R.string.preferences_advanced_data),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_clear_database),
                    subtitle = stringResource(R.string.pref_clear_database_summary),
                    onClick = {
                        navController.navigate(AdvancedSettingsRoutes.CLEAR_DATABASE)
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
        val context = LocalContext.current
        val networkHelper = remember { Injekt.get<NetworkHelper>() }
        return Preference.PreferenceCategory(
            title = stringResource(id = R.string.category_network),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_clear_cookies),
                    onClick = {
                        networkHelper.cookieManager.removeAll()
                        UiToasts.showToast(R.string.cookies_cleared)
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_clear_webview_data),
                    onClick = {
                        try {
                            WebView(context).run {
                                setDefaultSettings()
                                clearCache(true)
                                clearFormData()
                                clearHistory()
                                clearSslPreferences()
                            }
                            WebStorage.getInstance().deleteAllData()
                            context.applicationInfo?.dataDir?.let { File("$it/app_webview/").deleteRecursively() }
                            UiToasts.showToast(R.string.webview_data_deleted)
                        } catch (e: Throwable) {
                            Log.e("AdvancedNetworkSettings", e.stackTraceToString())
                            UiToasts.showToast(R.string.cache_delete_error)
                        }
                    },
                ),
                Preference.PreferenceItem.EditTextPreference(
                    value = advancedPreferences.userAgent,
                    title = stringResource(R.string.pref_user_agent_string),
                    onValueChanged = {
                        try {
                            // OkHttp checks for valid values internally
                            Headers.Builder().add("User-Agent", it)
                        } catch (_: IllegalArgumentException) {
                            UiToasts.showToast(R.string.error_user_agent_string_invalid)
                            return@EditTextPreference false
                        }
                        advancedPreferencesState.setValue(
                            advancedPreferences.copy(
                                userAgent = it
                            )
                        )
                        UiToasts.showToast(R.string.requires_app_restart)
                        true
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_reset_user_agent_string),
                    enabled = remember(advancedPreferences.userAgent) { advancedPreferences.userAgent != NetworkHelper.DEFAULT_USER_AGENT },
                    onClick = {
                        advancedPreferencesState.setValue(
                            advancedPreferences.copy(
                                userAgent = NetworkHelper.DEFAULT_USER_AGENT
                            )
                        )
                        UiToasts.showToast(R.string.requires_app_restart)
                    },
                ),
            )
        )
    }

    @Composable
    private fun getBackgroundJobGroup(): Preference.PreferenceCategory {
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current

        return Preference.PreferenceCategory(
            title = stringResource(id = R.string.label_background_activity),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_disable_battery_optimization),
                    subtitle = stringResource(R.string.pref_disable_battery_optimization_summary),
                    onClick = {
                        val packageName: String = context.packageName
                        if (!context.powerManager.isIgnoringBatteryOptimizations(packageName)) {
                            try {
                                @SuppressLint("BatteryLife")
                                val intent = Intent().apply {
                                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                    data = "package:$packageName".toUri()
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                UiToasts.showToast(R.string.battery_optimization_setting_activity_not_found)
                            }
                        } else {
                            UiToasts.showToast(R.string.battery_optimization_disabled)
                        }
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = "Don't kill my app!",
                    subtitle = stringResource(R.string.about_dont_kill_my_app),
                    onClick = { uriHandler.openUri("https://dontkillmyapp.com/") },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.advanced_worker_infos),
                    onClick = { navController.navigate(AdvancedSettingsRoutes.PROCESS_INFOS) },
                ),
            )
        )
    }
}