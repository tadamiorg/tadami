package com.sf.tadami.crash

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.widget.Toast
import androidx.core.content.getSystemService
import com.sf.tadami.R
import com.sf.tadami.extension.ExtensionManager
import com.sf.tadami.extension.util.ExtensionsGuard
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.utils.logcat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import logcat.LogPriority
import kotlin.system.exitProcess

class GlobalExceptionHandler private constructor(
    private val applicationContext: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler,
    private val activityToBeLaunched: Class<*>,
) : Thread.UncaughtExceptionHandler {

    object ThrowableSerializer : KSerializer<Throwable> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("Throwable", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): Throwable =
            Throwable(message = decoder.decodeString())

        override fun serialize(encoder: Encoder, value: Throwable) =
            encoder.encodeString(value.stackTraceToString())
    }

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        val timestamp = System.currentTimeMillis()
        val culprits = ExtensionsGuard.culpritsOf(exception)
        // The main thread can't go on without its looper, another one can end alone
        if (culprits.isNotEmpty() && thread !== Looper.getMainLooper().thread &&
            disableCrashedExtensions(culprits, exception, timestamp)
        ) {
            return
        }
        // First, so that the next launch doesn't crash the same way
        ExtensionsGuard.onUncaughtException(applicationContext, culprits)
        // Here: the crash screen's process doesn't load the extensions
        val culpritsInfo = runCatching { culpritsInfo(culprits) }.getOrDefault(emptyList())
        CrashReporter.writeCrash(applicationContext, exception, timestamp, culprits = culpritsInfo)
        try {
            logcat(priority = LogPriority.ERROR, throwable = exception)
            launchActivity(applicationContext, activityToBeLaunched, exception, timestamp, culpritsInfo)
            exitProcess(0)
        } catch (_: Exception) {
            defaultHandler.uncaughtException(thread, exception)
        }
    }

    /**
     * Lets the app go on after [exception], coming from the code of the [culprits] extensions, ended
     * a thread other than the main one: they're quarantined and their sources unloaded, the report
     * saved, and the user told. False when that failed: the app crashes as usual.
     */
    private fun disableCrashedExtensions(culprits: Set<String>, exception: Throwable, timestamp: Long): Boolean {
        return runCatching {
            ExtensionsGuard.quarantine(applicationContext, culprits)
            // Not built yet while its startup loads them: it leaves the quarantined ones out then
            ExtensionManager.instance?.disableCrashedExtensions(culprits)
            logcat(LogPriority.ERROR, exception) { "Extensions disabled after a crash: $culprits" }
            // Shareable from the advanced settings: no need to offer it on the next launch, the
            // user is told now
            CrashReporter.writeCrash(applicationContext, exception, timestamp, offered = true, culprits = culpritsInfo(culprits))
            val names = culprits.joinToString { ExtensionsGuard.nameOf(it) }
            UiToasts.showToast(R.string.ext_crashed_disabled, Toast.LENGTH_LONG, names)
            true
        }.getOrDefault(false)
    }

    private fun culpritsInfo(culprits: Set<String>): List<CrashReporter.Culprit> {
        return culprits.map { CrashReporter.Culprit(ExtensionsGuard.nameOf(it), ExtensionsGuard.versionNameOf(it)) }
    }

    private fun launchActivity(
        applicationContext: Context,
        activity: Class<*>,
        exception: Throwable,
        timestamp: Long,
        culprits: List<CrashReporter.Culprit>,
    ) {
        // The task of the app, its activities gone with this process, and the one of a previous
        // crash screen: the crash screen starts a new task, and is the only one left in recents
        runCatching {
            applicationContext.getSystemService<ActivityManager>()?.appTasks?.forEach { it.finishAndRemoveTask() }
        }
        val intent = Intent(applicationContext, activity).apply {
            putExtra(INTENT_EXTRA, Json.encodeToString(ThrowableSerializer, exception))
            putExtra(TIMESTAMP_EXTRA, timestamp)
            putExtra(CULPRITS_EXTRA, Json.encodeToString(culpritsSerializer, culprits))
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        applicationContext.startActivity(intent)
    }

    companion object {
        private const val INTENT_EXTRA = "Throwable"
        private const val TIMESTAMP_EXTRA = "Timestamp"
        private const val CULPRITS_EXTRA = "Culprits"

        private val culpritsSerializer = ListSerializer(CrashReporter.Culprit.serializer())

        fun initialize(
            applicationContext: Context,
            activityToBeLaunched: Class<*>,
        ) {
            val handler = GlobalExceptionHandler(
                applicationContext,
                Thread.getDefaultUncaughtExceptionHandler() as Thread.UncaughtExceptionHandler,
                activityToBeLaunched,
            )
            Thread.setDefaultUncaughtExceptionHandler(handler)
        }

        /** The extensions the crash of [intent] comes from, see [CrashReporter.CrashLog.culprits]. */
        fun getCulpritsFromIntent(intent: Intent): List<CrashReporter.Culprit> = runCatching {
            Json.decodeFromString(culpritsSerializer, intent.getStringExtra(CULPRITS_EXTRA)!!)
        }.getOrDefault(emptyList())

        /** When the crash of [intent] happened, to tell its report from an older one. */
        fun getTimestampFromIntent(intent: Intent): Long = intent.getLongExtra(TIMESTAMP_EXTRA, -1L)

        fun getThrowableFromIntent(intent: Intent): Throwable? {
            return try {
                Json.decodeFromString(ThrowableSerializer, intent.getStringExtra(INTENT_EXTRA)!!)
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Wasn't able to retrieve throwable from intent" }
                null
            }
        }
    }
}
