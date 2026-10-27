package com.sf.tadami.ui.tabs.more.settings.screens.advanced

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.sf.tadami.BackgroundJobs
import com.sf.tadami.navigation.graphs.settings.AdvancedSettingsRoutes
import com.sf.tadami.preferences.advanced.AdBlockPreferences
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.shared.resources.*
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.webview.WebViewAdBlocker
import com.sf.tadami.ui.webview.adblock.FilterList
import com.sf.tadami.utils.powerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

/* The groups of the advanced settings only Android has: the in-app browser's ad block, the background work. */

@Composable
fun adBlockGroup(): Preference.PreferenceCategory {
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
        updating -> stringResource(Res.string.pref_webview_adblock_updating)
        status?.compiledLists == null -> stringResource(Res.string.pref_webview_adblock_never_updated)
        else -> {
            val current = status!!
            val summary = stringResource(
                Res.string.pref_webview_adblock_last_update,
                if (current.lastSuccess > 0) {
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(current.lastSuccess))
                } else {
                    "-"
                },
                pluralStringResource(Res.plurals.pref_webview_adblock_rules, current.ruleCount, numberFormat.format(current.ruleCount)),
            )
            if (current.lastAttempt > current.lastSuccess) {
                stringResource(Res.string.pref_webview_adblock_last_attempt_failed, summary)
            } else {
                summary
            }
        }
    }

    return Preference.PreferenceCategory(
        title = stringResource(Res.string.pref_category_webview_adblock),
        preferenceItems = listOf<Preference.PreferenceItem<*>>(
            Preference.PreferenceItem.TogglePreference(
                value = adBlock.enabled,
                title = stringResource(Res.string.pref_webview_adblock),
                subtitle = stringResource(Res.string.pref_webview_adblock_summary),
                onValueChanged = { enabled ->
                    adBlockState.setValue(adBlock.copy(enabled = enabled))
                    Injekt.get<BackgroundJobs>().scheduleAdBlockUpdates(enabled)
                    true
                },
            ),
        ) + FilterList.entries.map { list ->
            val ruleCount = status?.lists?.get(list.id)?.ruleCount ?: 0
            Preference.PreferenceItem.TogglePreference(
                value = list.id !in adBlock.disabledLists,
                title = list.title,
                subtitle = if (ruleCount > 0) {
                    pluralStringResource(Res.plurals.pref_webview_adblock_rules, ruleCount, numberFormat.format(ruleCount))
                } else {
                    stringResource(Res.string.pref_webview_adblock_list_not_downloaded)
                },
                enabled = adBlock.enabled,
                onValueChanged = { on ->
                    val disabled = if (on) adBlock.disabledLists - list.id else adBlock.disabledLists + list.id
                    adBlockState.setValue(adBlock.copy(disabledLists = disabled))
                    // Compiled again, from the lists already downloaded when they're unchanged
                    Injekt.get<BackgroundJobs>().startAdBlockUpdate(manual = false)
                    true
                },
            )
        } + Preference.PreferenceItem.TextPreference(
            title = stringResource(Res.string.pref_webview_adblock_update_now),
            subtitle = updateSummary,
            enabled = !updating,
            onClick = {
                Injekt.get<BackgroundJobs>().startAdBlockUpdate(manual = true)
                UiToasts.showToast(Res.string.pref_webview_adblock_update_started)
            },
        ),
    )
}

@Composable
fun backgroundJobGroup(navController: NavHostController): Preference.PreferenceCategory {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    return Preference.PreferenceCategory(
        title = stringResource(Res.string.label_background_activity),
        preferenceItems = listOf(
            Preference.PreferenceItem.TextPreference(
                title = stringResource(Res.string.pref_disable_battery_optimization),
                subtitle = stringResource(Res.string.pref_disable_battery_optimization_summary),
                onClick = {
                    val packageName: String = context.packageName
                    if (!context.powerManager.isIgnoringBatteryOptimizations(packageName)) {
                        try {
                            val intent = Intent().apply {
                                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                data = "package:$packageName".toUri()
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            UiToasts.showToast(Res.string.battery_optimization_setting_activity_not_found)
                        }
                    } else {
                        UiToasts.showToast(Res.string.battery_optimization_disabled)
                    }
                },
            ),
            Preference.PreferenceItem.TextPreference(
                title = "Don't kill my app!",
                subtitle = stringResource(Res.string.about_dont_kill_my_app),
                onClick = { uriHandler.openUri("https://dontkillmyapp.com/") },
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(Res.string.advanced_worker_infos),
                onClick = { navController.navigate(AdvancedSettingsRoutes.PROCESS_INFOS) },
            ),
        )
    )
}
