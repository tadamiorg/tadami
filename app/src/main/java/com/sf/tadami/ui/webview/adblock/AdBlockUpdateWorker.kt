package com.sf.tadami.ui.webview.adblock

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sf.tadami.R
import com.sf.tadami.preferences.advanced.AdBlockPreferences
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.webview.WebViewAdBlocker
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.workManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

/** Keeps the WebView ad blocker's filter lists up to date. */
class AdBlockUpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val manual = inputData.getBoolean(KEY_MANUAL, false)
        val preferences = Injekt.get<DataStore<Preferences>>().getPreferencesGroup(AdBlockPreferences)
        // Asked from the settings: the lists are fetched even while the blocker is off
        if (!preferences.enabled && !manual) return@withContext Result.success()
        try {
            val result = WebViewAdBlocker.update(applicationContext, preferences.lists)
            val state = result.state
            Log.i(
                TAG,
                "Filter lists ${if (result.rules != null) "compiled" else "unchanged"}: ${state.ruleCount} rules, " +
                    "${state.blockedHosts} blocked hosts, ${state.exceptionHosts} exception hosts, " +
                    "${state.compiledSize / 1024} KiB, failed: ${result.failed.map { it.id }}",
            )
            if (manual) {
                UiToasts.showToast(
                    if (result.failed.isEmpty()) R.string.pref_webview_adblock_updated else R.string.pref_webview_adblock_update_failed,
                )
            }
            if (result.failed.isNotEmpty() && !manual) Result.retry() else Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Could not update the filter lists", e)
            if (manual) UiToasts.showToast(R.string.pref_webview_adblock_update_failed)
            if (manual) Result.failure() else Result.retry()
        }
    }

    companion object {
        private const val TAG = "AdBlockUpdate"
        private const val WORK_NAME_AUTO = "webview_adblock_update_auto"
        private const val WORK_NAME_NOW = "webview_adblock_update_now"
        private const val KEY_MANUAL = "manual"

        /** Every few days, on an unmetered network like the library updates by default. */
        fun setupTask(context: Context, enabled: Boolean? = null) {
            val isEnabled = enabled ?: runBlocking {
                Injekt.get<DataStore<Preferences>>().getPreferencesGroup(AdBlockPreferences).enabled
            }
            if (!isEnabled) {
                context.workManager.cancelUniqueWork(WORK_NAME_AUTO)
                return
            }
            val request = PeriodicWorkRequestBuilder<AdBlockUpdateWorker>(
                AdBlockUpdater.UPDATE_INTERVAL_DAYS,
                TimeUnit.DAYS,
            )
                .setConstraints(
                    Constraints(
                        requiredNetworkType = NetworkType.UNMETERED,
                        requiresBatteryNotLow = true,
                    ),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
            context.workManager.enqueueUniquePeriodicWork(WORK_NAME_AUTO, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        /** As soon as a network is there. [manual]: asked by the user, who gets told how it went. */
        fun startNow(context: Context, manual: Boolean) {
            val request = OneTimeWorkRequestBuilder<AdBlockUpdateWorker>()
                .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                .setInputData(workDataOf(KEY_MANUAL to manual))
                .build()
            context.workManager.enqueueUniqueWork(
                WORK_NAME_NOW,
                if (manual) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                request,
            )
        }

        /**
         * As the app starts: the lists were never compiled, or other lists are enabled. The blocker
         * blocks nothing until then. Blocking.
         */
        fun downloadIfNeeded(context: Context) {
            val preferences = runBlocking {
                Injekt.get<DataStore<Preferences>>().getPreferencesGroup(AdBlockPreferences)
            }
            if (!preferences.enabled) return
            if (AdBlockUpdater.needsDownload(WebViewAdBlocker.readState(context), preferences.lists)) {
                startNow(context, manual = false)
            }
        }

        /** When the WebView opens: the lists were never fetched, changed, or are stale. Blocking. */
        fun updateIfNeeded(context: Context, preferences: AdBlockPreferences) {
            if (!preferences.enabled) return
            val state = WebViewAdBlocker.readState(context)
            if (AdBlockUpdater.needsUpdate(state, preferences.lists, System.currentTimeMillis())) {
                startNow(context, manual = false)
            }
        }
    }
}
