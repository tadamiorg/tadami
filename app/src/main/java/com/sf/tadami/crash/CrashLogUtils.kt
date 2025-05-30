package com.sf.tadami.crash

import android.content.Context
import android.os.Build
import com.sf.tadami.BuildConfig
import com.sf.tadami.extension.ExtensionManager
import com.sf.tadami.network.utils.WebViewUtil
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.utils.getUriCompat
import com.sf.tadami.ui.utils.toShareIntent
import com.sf.tadami.utils.createFileInCacheDir
import com.sf.tadami.utils.withNonCancellableContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CrashLogUtil(
    private val context: Context,
    private val extensionManager: ExtensionManager = Injekt.get(),
) {

    suspend fun dumpLogs() = withNonCancellableContext {
        try {
            val file = context.createFileInCacheDir("tadami_crash_logs.txt")

            file.appendText(getDebugInfo() + "\n\n")
            getExtensionsInfo()?.let { file.appendText("$it\n\n") }

            Runtime.getRuntime().exec("logcat *:E -d -f ${file.absolutePath}").waitFor()

            val uri = file.getUriCompat(context)
            context.startActivity(uri.toShareIntent(context, "text/plain"))
        } catch (e: Throwable) {
            UiToasts.showToast("Failed to get logs")
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

        return if (extensionInfoList.isNotEmpty()) {
            (listOf("Problematic extensions:") + extensionInfoList)
                .joinToString("\n")
        } else {
            null
        }
    }
}
