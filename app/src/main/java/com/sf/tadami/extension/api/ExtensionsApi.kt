package com.sf.tadami.extension.api

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.ExtensionManager
import com.sf.tadami.extension.util.ExtensionsLoader
import com.sf.tadami.network.GET
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.network.awaitSuccess
import com.sf.tadami.network.parseAs
import com.sf.tadami.notifications.extensionsinstaller.ExtensionInstallerNotifier
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.utils.Lang
import com.sf.tadami.utils.editPreference
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.logcat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.injectLazy
import java.time.Instant
import kotlin.time.Duration.Companion.days

internal class ExtensionsApi {

    private val networkService: NetworkHelper by injectLazy()
    private val dataStore: DataStore<Preferences> by injectLazy()
    private val extensionManager: ExtensionManager by injectLazy()
    private val json: Json by injectLazy()

    // Read on demand from the calling coroutine. This used to be a `runBlocking` field
    // initializer, which ran on whatever thread built ExtensionManager — the main thread at
    // startup.
    private suspend fun sourcePreferences() = dataStore.getPreferencesGroup(SourcesPreferences)

    suspend fun findExtensions(): List<Extension.Available> {
        return withContext(Dispatchers.IO) {
            sourcePreferences().extensionsRepos.flatMap { getExtensions(it) }
        }
    }

    private suspend fun getExtensions(repoBaseUrl: String): List<Extension.Available> {
        return try {
            val response = networkService.client
                .newCall(GET("$repoBaseUrl/index.min.json"))
                .awaitSuccess()

            with(json) {
                response
                    .parseAs<List<ExtensionJsonObject>>()
                    .toExtensions(repoBaseUrl)
            }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Failed to get extensions from $repoBaseUrl" }
            emptyList()
        }
    }

    suspend fun checkForUpdates(
        context: Context,
        fromAvailableExtensionList: Boolean = false,
    ): List<Extension.Installed>? {
        val now = Instant.now().toEpochMilli()
        val lastExtCheck = sourcePreferences().lastExtCheck

        // Limit checks to once a day at most
        if (!fromAvailableExtensionList &&
            now < lastExtCheck + 1.days.inWholeMilliseconds
        ) {
            return null
        }

        val extensions = if (fromAvailableExtensionList) {
            extensionManager.availableExtensionsFlow.value
        } else {
            // Stamp the check with "now"; writing lastExtCheck back left it at 0 forever, so the
            // daily throttle above never actually engaged.
            findExtensions().also { dataStore.editPreference(now, SourcesPreferences.LAST_EXT_CHECK) }
        }

        val installedExtensions = extensionManager.installedExtensionsFlow.value

        val extensionsWithUpdate = mutableListOf<Extension.Installed>()
        for (installedExt in installedExtensions) {
            val pkgName = installedExt.pkgName
            val availableExt = extensions.find { it.pkgName == pkgName } ?: continue
            val hasUpdatedVer = availableExt.versionCode > installedExt.versionCode
            val hasUpdatedLib = availableExt.apiVersion > installedExt.apiVersion
            val hasUpdate = hasUpdatedVer || hasUpdatedLib
            if (hasUpdate) {
                extensionsWithUpdate.add(installedExt)
            }
        }

        if (extensionsWithUpdate.isNotEmpty()) {
            ExtensionInstallerNotifier(context).promptUpdates(extensionsWithUpdate.map { it.name })
        }

        return extensionsWithUpdate
    }

    private fun List<ExtensionJsonObject>.toExtensions(repoUrl: String): List<Extension.Available> {
        return this
            .filter {
                val libVersion = it.extractLibVersion()
                libVersion >= ExtensionsLoader.API_VERSION_MIN && libVersion <= ExtensionsLoader.API_VERSION_MAX
            }
            .map {
                Extension.Available(
                    name = it.name.substringAfter("Tadami: "),
                    pkgName = it.pkg,
                    versionName = it.version,
                    versionCode = it.code,
                    apiVersion = it.extractLibVersion(),
                    lang = langMapper(it.lang),
                    sources = it.sources?.map(extensionSourceMapper).orEmpty(),
                    apkName = it.apk,
                    iconUrl = "$repoUrl/icon/${it.pkg}.png",
                    repoUrl = repoUrl,
                )
            }
    }



    fun getApkUrl(extension: Extension.Available): String {
        return "${extension.repoUrl}/apk/${extension.apkName}"
    }

    private fun ExtensionJsonObject.extractLibVersion(): Double {
        return version.substringBeforeLast('.').toDouble()
    }
}

@Serializable
private data class ExtensionJsonObject(
    val name: String,
    val pkg: String,
    val apk: String,
    val lang: String,
    val code: Long,
    val version: String,
    val sources: List<ExtensionSourceJsonObject>?,
)

@Serializable
private data class ExtensionSourceJsonObject(
    val id: Long,
    val lang: String,
    val name: String,
    val baseUrl: String,
)

private fun langMapper(lang : String) : Lang{
    return when(lang){
        "en" -> Lang.ENGLISH
        "fr" -> Lang.FRENCH
        else -> Lang.UNKNOWN
    }
}

private val extensionSourceMapper: (ExtensionSourceJsonObject) -> Extension.Available.Source = {
    Extension.Available.Source(
        id = it.id,
        lang = langMapper(it.lang),
        name = it.name,
        baseUrl = it.baseUrl,
    )
}