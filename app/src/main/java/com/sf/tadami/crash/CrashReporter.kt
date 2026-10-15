package com.sf.tadami.crash

import android.content.Context
import android.os.Build
import com.sf.tadami.BuildConfig
import com.sf.tadami.extension.ExtensionManager
import com.sf.tadami.network.utils.WebViewUtil
import com.sf.tadami.ui.utils.getUriCompat
import com.sf.tadami.ui.utils.toShareIntent
import com.sf.tadami.utils.createFileInCacheDir
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Writes the uncaught exception to disk, with what is needed to report it, so the crash screen can
 * share it without running the code of the app again, the extensions in particular.
 */
object CrashReporter {

    private const val MAX_STACKTRACE_LENGTH = 24_000

    // An older report is unlikely to still be useful
    private const val MAX_REPORT_AGE_MS = 7L * 24 * 60 * 60 * 1000

    private val json = Json { ignoreUnknownKeys = true }

    /** An extension a crash comes from, see [com.sf.tadami.extension.util.ExtensionsGuard.culpritsOf]. */
    @Serializable
    data class Culprit(val name: String, val versionName: String)

    @Serializable
    data class CrashLog(
        val stacktrace: String,
        val logs: String = "",
        val versionName: String,
        val versionCode: Int,
        val buildDate: String,
        val androidVersion: String,
        val device: String,
        val webView: String,
        val extensions: String,
        val timestamp: Long,
        /** Whether the crash screen or the next launch already offered it */
        val offered: Boolean = false,
        /** The extensions it comes from, quarantined */
        val culprits: List<Culprit> = emptyList(),
    ) {
        /**
         * @param withCulprits Whether it starts with the [culprits], left out where they're shown
         * already, see [CrashScreen].
         */
        fun format(withCulprits: Boolean = true): String = buildString {
            if (withCulprits) {
                culprits.forEach {
                    appendLine("Caused by the ${it.name} extension (${it.versionName}). It has been disabled; retry it in Extensions.")
                }
                if (culprits.isNotEmpty()) appendLine()
            }
            appendLine("App version: $versionName ($versionCode, $buildDate)")
            appendLine("Android version: $androidVersion")
            appendLine("Device: $device")
            appendLine("WebView: $webView")
            if (extensions.isNotBlank()) {
                appendLine()
                appendLine(extensions)
            }
            appendLine()
            appendLine(stacktrace)
            if (logs.isNotBlank()) {
                appendLine()
                appendLine("--- Recent logs ---")
                appendLine(logs)
            }
        }
    }

    private fun crashFile(context: Context) = File(context.filesDir, "crash/last_crash.json")

    /**
     * Never throws: a failure here would replace the crash being reported.
     *
     * @param offered Whether the user was already told about it, see [CrashLog.offered].
     * @param culprits The extensions it comes from, see [CrashLog.culprits].
     */
    fun writeCrash(
        context: Context,
        throwable: Throwable,
        timestamp: Long,
        offered: Boolean = false,
        culprits: List<Culprit> = emptyList(),
    ) {
        runCatching {
            val log = CrashLog(
                stacktrace = throwable.stackTraceToString().take(MAX_STACKTRACE_LENGTH),
                logs = CrashLogBuffer.snapshot(),
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE,
                buildDate = BuildConfig.BUILD_DATE,
                androidVersion = "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}; build ${Build.DISPLAY})",
                device = "${Build.MANUFACTURER} ${Build.BRAND} ${Build.MODEL} (${Build.DEVICE}, ${Build.PRODUCT})",
                webView = runCatching { WebViewUtil.getVersion(context) }.getOrDefault("?"),
                // Only if already built: building it now would load the extensions again
                extensions = runCatching {
                    ExtensionManager.instance?.let { getExtensionsInfo(it) }
                }.getOrNull().orEmpty(),
                timestamp = timestamp,
                offered = offered,
                culprits = culprits,
            )
            write(context, log)
        }
    }

    private fun write(context: Context, log: CrashLog) {
        val file = crashFile(context)
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(CrashLog.serializer(), log))
    }

    /** The report not shared yet, if not expired. */
    fun readPending(context: Context): CrashLog? = runCatching {
        val file = crashFile(context).takeIf { it.exists() } ?: return null
        val log = json.decodeFromString(CrashLog.serializer(), file.readText())
        if (isExpired(log, System.currentTimeMillis(), BuildConfig.VERSION_CODE)) {
            file.delete()
            return null
        }
        log
    }.getOrNull()

    /** Stops offering [log] on the next launch, it stays shareable from the advanced settings. */
    fun markOffered(context: Context, log: CrashLog) {
        if (log.offered) return
        runCatching { write(context, log.copy(offered = true)) }
    }

    /** Too old, or from another version of the app, whose code may have changed since. */
    internal fun isExpired(log: CrashLog, now: Long, versionCode: Int): Boolean =
        log.versionCode != versionCode || now - log.timestamp > MAX_REPORT_AGE_MS

    fun clearPending(context: Context) {
        runCatching { crashFile(context).delete() }
    }

    /**
     * Shares [report] as a text file.
     *
     * @param fromCrashScreen Whether it's shared from the crash screen, given by its process then,
     * see [CrashReportProvider].
     */
    fun shareReport(context: Context, report: String, fromCrashScreen: Boolean = false): Boolean = runCatching {
        val file = context.createFileInCacheDir("tadami_crash_logs.txt")
        file.writeText(report)
        val uri = if (fromCrashScreen) CrashReportProvider.getUri(context, file) else file.getUriCompat(context)
        context.startActivity(uri.toShareIntent(context, "text/plain"))
        true
    }.getOrDefault(false)

    fun getExtensionsInfo(extensionManager: ExtensionManager): String? {
        val availableExtensions = extensionManager.availableExtensionsFlow.value.associateBy { it.pkgName }

        val extensionInfoList = extensionManager.installedExtensionsFlow.value
            .sortedBy { it.name }
            .mapNotNull {
                val availableExtension = availableExtensions[it.pkgName]
                val hasUpdate = availableExtension != null && it.isUpdatedBy(availableExtension)

                if (!hasUpdate && !it.isObsolete && it.loadIssue == null) return@mapNotNull null

                """
                    - ${it.name}
                      Installed: ${it.versionName} / Available: ${availableExtension?.versionName ?: "?"}
                      Obsolete: ${it.isObsolete}
                      Load issue: ${it.loadIssue ?: "none"}
                """.trimIndent()
            }

        return if (extensionInfoList.isNotEmpty()) {
            (listOf("Problematic extensions:") + extensionInfoList)
                .joinToString("\n")
        } else {
            null
        }
    }
}
