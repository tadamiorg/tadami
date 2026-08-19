package com.sf.tadami.crash

import android.content.Context
import android.os.Build
import com.sf.tadami.BuildConfig
import com.sf.tadami.R
import com.sf.tadami.extension.ExtensionManager
import com.sf.tadami.network.utils.WebViewUtil
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.utils.getUriCompat
import com.sf.tadami.ui.utils.toShareIntent
import com.sf.tadami.utils.createFileInCacheDir
import com.sf.tadami.utils.logcat
import com.sf.tadami.utils.withNonCancellableContext
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CrashLogUtil(
    private val context: Context,
    private val extensionManager: ExtensionManager = Injekt.get(),
) {

    suspend fun dumpLogs() = withNonCancellableContext {
        try {
            val file = context.createFileInCacheDir("tadami_crash_logs.txt")

            // The persisted crash report, when there is one, is the most useful part: it has the
            // stack trace plus the log lines from just before the crash.
            CrashReporter.readPending(context)?.let {
                file.appendText(it.format() + "\n\n")
            }

            file.appendText(getDebugInfo() + "\n\n")
            getExtensionsInfo()?.let { file.appendText("$it\n\n") }

            val recentLogs = CrashLogBuffer.snapshot()
            if (recentLogs.isNotBlank()) {
                file.appendText("--- Recent logs ---\n$recentLogs\n\n")
            }

            // System logcat as a last resort. Most of the app's own Log.d calls are stripped from
            // release builds, which is why the in-memory buffer above exists.
            runCatching {
                Runtime.getRuntime().exec("logcat *:E -d").inputStream.use { stream ->
                    file.appendText("--- System logcat ---\n")
                    file.appendBytes(stream.readBytes())
                }
            }

            val uri = file.getUriCompat(context)
            context.startActivity(uri.toShareIntent(context, "text/plain"))
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Failed to dump crash logs" }
            UiToasts.showToast(R.string.crash_logs_dump_failed)
        }
    }

    private fun getDebugInfo(): String {
        return """
            App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}, ${BuildConfig.BUILD_DATE})
            Android version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}; build ${Build.DISPLAY})
            Device brand: ${Build.BRAND}
            Device manufacturer: ${Build.MANUFACTURER}
            Device name: ${Build.DEVICE} (${Build.PRODUCT})
            Device model: ${Build.MODEL}
            WebView: ${WebViewUtil.getVersion(context)}
        """.trimIndent()
    }

    private fun getExtensionsInfo(): String? {
        val availableExtensions = extensionManager.availableExtensionsFlow.value.associateBy { it.pkgName }

        val extensionInfoList = extensionManager.installedExtensionsFlow.value
            .sortedBy { it.name }
            .mapNotNull {
                val availableExtension = availableExtensions[it.pkgName]
                val hasUpdate = (availableExtension?.versionCode ?: 0) > it.versionCode

                if (!hasUpdate && !it.isObsolete) return@mapNotNull null

                """
                    - ${it.name}
                      Installed: ${it.versionName} / Available: ${availableExtension?.versionName ?: "?"}
                      Obsolete: ${it.isObsolete}
                """.trimIndent()
            }

        val failedList = extensionManager.failedExtensionsFlow.value.map {
            "- ${it.extName ?: it.pkgName} failed to load: ${it.reason}"
        }

        val sections = buildList {
            if (extensionInfoList.isNotEmpty()) {
                add((listOf("Problematic extensions:") + extensionInfoList).joinToString("\n"))
            }
            if (failedList.isNotEmpty()) {
                add((listOf("Extensions that failed to load:") + failedList).joinToString("\n"))
            }
        }

        return sections.takeIf { it.isNotEmpty() }?.joinToString("\n\n")
    }
}
