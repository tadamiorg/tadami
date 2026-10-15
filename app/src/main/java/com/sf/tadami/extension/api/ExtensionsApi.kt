package com.sf.tadami.extension.api

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.ExtensionManager
import com.sf.tadami.extension.model.ExtensionLoadIssue
import com.sf.tadami.extension.model.LibVersion
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import uy.kohesive.injekt.injectLazy
import java.time.Instant
import kotlin.time.Duration.Companion.days

internal class ExtensionsApi {

    private val networkService: NetworkHelper by injectLazy()
    private val dataStore: DataStore<Preferences> by injectLazy()
    private val extensionManager: ExtensionManager by injectLazy()
    private val json: Json by injectLazy()

    // Read when needed: built with the extension manager, it would block whatever thread builds it
    private suspend fun sourcePreferences() = dataStore.getPreferencesGroup(SourcesPreferences)

    /**
     * The extensions of each repo. One that fails keeps its entries of [previous], the extensions
     * found before: it doesn't hide the others. When all of them fail, the error is propagated so
     * the caller can keep its previous state.
     */
    suspend fun findExtensions(previous: List<Extension.Available>): RepoExtensions {
        return withContext(Dispatchers.IO) {
            val repos = sourcePreferences().extensionsRepos
            val results = repos.associateWith { repo ->
                try {
                    Result.success(getExtensions(repo))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            if (results.isNotEmpty() && results.values.all { it.isFailure }) {
                throw results.values.first().exceptionOrNull()!!
            }
            RepoExtensions.of(results.mapValues { it.value.getOrNull() }, previous)
        }
    }

    /**
     * Network, HTTP and index format errors are propagated so the caller can tell the repo failed.
     */
    private suspend fun getExtensions(repoBaseUrl: String): List<Extension.Available> {
        return try {
            val response = networkService.client
                .newCall(GET("$repoBaseUrl/index.min.json"))
                .awaitSuccess()

            with(json) {
                response
                    .parseAs<List<JsonElement>>()
                    // Decode each entry on its own so one malformed entry doesn't hide the whole repo
                    .mapNotNull { entry ->
                        runCatching { json.decodeFromJsonElement(ExtensionJsonObject.serializer(), entry) }
                            .onFailure { Log.d("GetExtensions", "Skipping malformed entry from $repoBaseUrl: ${it.message}") }
                            .getOrNull()
                    }
                    .toExtensions(repoBaseUrl)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d("GetExtensions","Failed to get extensions from $repoBaseUrl")
            throw e
        }
    }

    suspend fun checkForUpdates(
        context: Context,
        fromAvailableExtensionList: Boolean = false,
    ): List<Extension.Installed>? {
        // Limit checks to once a day at most
        if (!fromAvailableExtensionList &&
            Instant.now().toEpochMilli() < sourcePreferences().lastExtCheck + 1.days.inWholeMilliseconds
        ) {
            return null
        }

        val extensions = if (fromAvailableExtensionList) {
            extensionManager.availableExtensionsFlow.value
        } else {
            findExtensions(extensionManager.availableExtensionsFlow.value).available.also {
                dataStore.editPreference(Instant.now().toEpochMilli(), SourcesPreferences.LAST_EXT_CHECK)
            }
        }

        val installedExtensions = extensionManager.installedExtensionsFlow.value

        val extensionsWithUpdate = mutableListOf<Extension.Installed>()
        for (installedExt in installedExtensions) {
            // Trusted first, then updated
            if (installedExt.loadIssue == ExtensionLoadIssue.UNTRUSTED) continue
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
            .mapNotNull {
                val libVersion = LibVersion.fromVersionName(it.version) ?: return@mapNotNull null
                Extension.Available(
                    name = it.name.substringAfter("Tadami: "),
                    pkgName = it.pkg,
                    versionName = it.version,
                    versionCode = it.code,
                    apiVersion = libVersion,
                    lang = Lang.fromCode(it.lang),
                    sources = it.sources?.map(extensionSourceMapper).orEmpty(),
                    apkName = it.apk,
                    iconUrl = "$repoUrl/icon/${it.pkg}.png",
                    repoUrl = repoUrl,
                    size = it.size,
                    sha256 = it.sha256,
                )
            }
    }



    fun getApkUrl(extension: Extension.Available): String {
        return "${extension.repoUrl}/apk/${extension.apkName}"
    }
}

/**
 * The extensions the repos list.
 *
 * @param failedRepos The repos that couldn't be read, see [isObsolete].
 */
internal class RepoExtensions(
    listed: List<Extension.Available>,
    private val failedRepos: Set<String> = emptySet(),
) {
    /** The ones this app can load, the others can't be installed nor be an update. */
    val available = listed.filter { ExtensionsLoader.libVersionIssue(it.apiVersion) == null }

    /** All of them: one made for a lib version this app can't load is still in the repo, not removed. */
    val listedPkgNames = listed.mapTo(HashSet()) { it.pkgName }

    /**
     * Whether the installed extension [pkgName], from the repo [repoUrl] (null when unknown), was
     * removed from the repos. Null when it can't be told: it may come from a repo that failed.
     */
    fun isObsolete(pkgName: String, repoUrl: String?): Boolean? = when {
        pkgName in listedPkgNames -> false
        failedRepos.isEmpty() -> true
        repoUrl != null && repoUrl !in failedRepos -> true
        else -> null
    }

    companion object {
        /**
         * @param byRepo The extensions of each repo, null for one that failed: it keeps its entries
         * of [previous], none on a cold start.
         */
        fun of(byRepo: Map<String, List<Extension.Available>?>, previous: List<Extension.Available>): RepoExtensions {
            val listed = byRepo.flatMap { (repo, extensions) -> extensions ?: previous.filter { it.repoUrl == repo } }
            return RepoExtensions(listed, failedRepos = byRepo.filterValues { it == null }.keys)
        }
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
    /** The hex SHA-256 of the APK, absent from older indexes. */
    val sha256: String? = null,
    /** The size in bytes of the APK, absent from older indexes. */
    val size: Long? = null,
)

@Serializable
private data class ExtensionSourceJsonObject(
    val id: Long,
    val lang: String,
    val name: String,
    val baseUrl: String = "",
)

private val extensionSourceMapper: (ExtensionSourceJsonObject) -> Extension.Available.Source = {
    Extension.Available.Source(
        id = it.id,
        lang = Lang.fromCode(it.lang),
        name = it.name,
        baseUrl = it.baseUrl,
    )
}