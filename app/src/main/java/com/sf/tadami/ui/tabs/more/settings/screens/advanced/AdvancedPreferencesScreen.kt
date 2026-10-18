package com.sf.tadami.ui.tabs.more.settings.screens.advanced

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.crash.CrashLogUtil
import com.sf.tadami.crash.CrashReporter
import com.sf.tadami.navigation.graphs.settings.AdvancedSettingsRoutes
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.network.utils.setDefaultSettings
import com.sf.tadami.preferences.advanced.AdBlockPreferences
import com.sf.tadami.preferences.advanced.AdvancedPreferences
import com.sf.tadami.preferences.model.DataStoreState
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.ui.tabs.more.settings.components.PreferenceScreen
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.webview.WebViewAdBlocker
import com.sf.tadami.ui.webview.adblock.AdBlockUpdateWorker
import com.sf.tadami.ui.webview.adblock.FilterList
import com.sf.tadami.utils.powerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Headers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

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
            getAdBlockGroup(),
            getBackgroundJobGroup()
        )
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    @Composable
    private fun getDataGroup(): Preference.PreferenceCategory {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
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
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_dump_crash_logs),
                    subtitle = stringResource(R.string.pref_dump_crash_logs_summary),
                    onClick = {
                        scope.launch {
                            val crashLog = withContext(Dispatchers.IO) { CrashReporter.readPending(context) }
                            if (crashLog == null) {
                                CrashLogUtil(context).dumpLogs()
                                return@launch
                            }
                            // The report as it was written, not this session's info
                            val shared = withContext(Dispatchers.IO) {
                                CrashReporter.shareReport(context, crashLog.format())
                            }
                            if (shared) {
                                withContext(Dispatchers.IO) { CrashReporter.clearPending(context) }
                            } else {
                                UiToasts.showToast(R.string.crash_logs_dump_failed)
                            }
                        }
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
                            WebView(context.applicationContext).run {
                                setDefaultSettings()
                                clearCache(true)
                                clearFormData()
                                clearHistory()
                                clearSslPreferences()
                                destroy()
                            }
                            WebStorage.getInstance().deleteAllData()
                            // Deleting app_webview/ while the WebView is loaded would leave it
                            // writing cookies to unlinked files, so clear them through the API
                            networkHelper.cookieManager.removeAll()
                            networkHelper.cookieManager.flush()
                            UiToasts.showToast(R.string.webview_data_deleted)
                        } catch (e: Throwable) {
                            Log.d("AdvancedNetworkSettings", e.stackTraceToString())
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
                        true
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_reset_user_agent_string),
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

    @Composable
    private fun getAdBlockGroup(): Preference.PreferenceCategory {
        val context = LocalContext.current
        val adBlockState = rememberDataStoreState(AdBlockPreferences)
        val adBlock by adBlockState.value.collectAsState()
        val status by WebViewAdBlocker.state.collectAsState()
        val updating by WebViewAdBlocker.updating.collectAsState()

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) { WebViewAdBlocker.readState(context.applicationContext) }
        }

        val numberFormat = remember { NumberFormat.getIntegerInstance() }
        val updateSummary = when {
            updating -> stringResource(R.string.pref_webview_adblock_updating)
            status?.compiledLists == null -> stringResource(R.string.pref_webview_adblock_never_updated)
            else -> {
                val current = status!!
                val summary = stringResource(
                    R.string.pref_webview_adblock_last_update,
                    if (current.lastSuccess > 0) {
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(current.lastSuccess))
                    } else {
                        "-"
                    },
                    pluralStringResource(R.plurals.pref_webview_adblock_rules, current.ruleCount, numberFormat.format(current.ruleCount)),
                )
                if (current.lastAttempt > current.lastSuccess) {
                    stringResource(R.string.pref_webview_adblock_last_attempt_failed, summary)
                } else {
                    summary
                }
            }
        }

        return Preference.PreferenceCategory(
            title = stringResource(id = R.string.pref_category_webview_adblock),
            preferenceItems = listOf<Preference.PreferenceItem<*>>(
                Preference.PreferenceItem.TogglePreference(
                    value = adBlock.enabled,
                    title = stringResource(R.string.pref_webview_adblock),
                    subtitle = stringResource(R.string.pref_webview_adblock_summary),
                    onValueChanged = { enabled ->
                        adBlockState.setValue(adBlock.copy(enabled = enabled))
                        AdBlockUpdateWorker.setupTask(context, enabled)
                        true
                    },
                ),
            ) + FilterList.entries.map { list ->
                val ruleCount = status?.lists?.get(list.id)?.ruleCount ?: 0
                Preference.PreferenceItem.TogglePreference(
                    value = list.id !in adBlock.disabledLists,
                    title = list.title,
                    subtitle = if (ruleCount > 0) {
                        pluralStringResource(R.plurals.pref_webview_adblock_rules, ruleCount, numberFormat.format(ruleCount))
                    } else {
                        stringResource(R.string.pref_webview_adblock_list_not_downloaded)
                    },
                    enabled = adBlock.enabled,
                    onValueChanged = { on ->
                        val disabled = if (on) adBlock.disabledLists - list.id else adBlock.disabledLists + list.id
                        adBlockState.setValue(adBlock.copy(disabledLists = disabled))
                        // Compiled again, from the lists already downloaded when they're unchanged
                        AdBlockUpdateWorker.startNow(context, manual = false)
                        true
                    },
                )
            } + Preference.PreferenceItem.TextPreference(
                title = stringResource(R.string.pref_webview_adblock_update_now),
                subtitle = updateSummary,
                enabled = !updating,
                onClick = {
                    AdBlockUpdateWorker.startNow(context, manual = true)
                    UiToasts.showToast(R.string.pref_webview_adblock_update_started)
                },
            ),
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