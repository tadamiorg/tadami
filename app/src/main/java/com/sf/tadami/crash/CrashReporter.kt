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
 * Persists uncaught exceptions to disk so a crash report survives the process dying.
 *
 * The previous implementation only put the throwable in the [android.content.Intent] that opens
 * [CrashActivity]; if that launch failed — or the user dismissed the screen — the crash was gone.
 * Writing it out first means the next normal launch can still offer the report.
 *
 * Mirrors the approach already used by the TV receiver app (`tadami-terebi`'s `CrashReporter`).
 */
object CrashReporter {

    private const val MAX_STACKTRACE_LEN = 24_000

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    @Serializable
    data class CrashLog(
        val stacktrace: String,
        val logs: String,
        val versionName: String,
        val versionCode: Int,
        val buildDate: String,
        val androidVersion: String,
        val device: String,
        val webView: String,
        val extensions: String,
        val timestamp: Long,
    ) {
        /** Human-readable form, used for both the crash screen and the shared file. */
        fun format(): String = buildString {
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

    private fun crashFile(context: Context): File =
        File(context.filesDir, "crash/last_crash.json").apply { parentFile?.mkdirs() }

    /**
     * Writes [throwable] to disk. Never throws — it runs from the uncaught-exception handler, where
     * a secondary failure would replace the real crash with a useless one.
     */
    fun writeCrash(context: Context, throwable: Throwable) {
        runCatching {
            val log = CrashLog(
                stacktrace = throwable.stackTraceToString().take(MAX_STACKTRACE_LEN),
                logs = CrashLogBuffer.snapshot(),
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE,
                buildDate = BuildConfig.BUILD_DATE,
                androidVersion = "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}; build ${Build.DISPLAY})",
                device = "${Build.MANUFACTURER} ${Build.BRAND} ${Build.MODEL} (${Build.DEVICE}, ${Build.PRODUCT})",
                webView = runCatching { WebViewUtil.getVersion(context) }.getOrDefault("?"),
                extensions = runCatching { problematicExtensions() }.getOrDefault(""),
                timestamp = System.currentTimeMillis(),
            )
            crashFile(context).writeText(json.encodeToString(CrashLog.serializer(), log))
        }
    }

    fun hasPending(context: Context): Boolean =
        runCatching { crashFile(context).let { it.exists() && it.length() > 0 } }.getOrDefault(false)

    fun readPending(context: Context): CrashLog? = runCatching {
        val raw = crashFile(context).takeIf { it.exists() }?.readText() ?: return null
        json.decodeFromString(CrashLog.serializer(), raw)
    }.getOrNull()

    fun clearPending(context: Context) {
        runCatching { crashFile(context).delete() }
    }

    /**
     * Shares the persisted report using nothing but the crash file.
     *
     * [CrashLogUtil] pulls in [ExtensionManager], which would instantiate every extension —
     * unacceptable from the ":error_handler" process, whose whole point is not to run the code
     * that just crashed. The report already contains the extension state captured at crash time.
     */
    fun shareReport(context: Context): Boolean = runCatching {
        val report = readPending(context) ?: return false
        val file = context.createFileInCacheDir("tadami_crash_logs.txt")
        file.writeText(report.format())
        context.startActivity(file.getUriCompat(context).toShareIntent(context, "text/plain"))
        true
    }.getOrDefault(false)

    /**
     * Extensions that are out of date or obsolete — by far the most common cause of crashes in a
     * Tachiyomi-style app, so worth having in every report.
     */
    private fun problematicExtensions(): String {
        // Deliberately not Injekt.get(): see ExtensionManager.instance.
        val extensionManager = ExtensionManager.instance ?: return ""
        val available = extensionManager.availableExtensionsFlow.value.associateBy { it.pkgName }

        val lines = extensionManager.installedExtensionsFlow.value
            .sortedBy { it.name }
            .mapNotNull {
                val availableExtension = available[it.pkgName]
                val hasUpdate = (availableExtension?.versionCode ?: 0) > it.versionCode
                if (!hasUpdate && !it.isObsolete) return@mapNotNull null

                """
                    - ${it.name}
                      Installed: ${it.versionName} / Available: ${availableExtension?.versionName ?: "?"}
                      Obsolete: ${it.isObsolete}
                """.trimIndent()
            }

        val failed = extensionManager.failedExtensionsFlow.value.map {
            "- ${it.extName ?: it.pkgName} failed to load: ${it.reason}"
        }

        return (
            (if (lines.isNotEmpty()) listOf("Problematic extensions:") + lines else emptyList()) +
                (if (failed.isNotEmpty()) listOf("Extensions that failed to load:") + failed else emptyList())
            ).joinToString("\n")
    }
}
