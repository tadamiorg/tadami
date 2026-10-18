package com.sf.tadami.ui.tabs.browse.tabs.extensions.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.ExtensionManager
import com.sf.tadami.extension.api.ExtensionRepos
import com.sf.tadami.extension.api.RepoMeta
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.sources.SourcesPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * The name of the repo [repoUrl] for the user: the official one for the app's own only, else the
 * name its [meta] gives if it can be shown, see [ExtensionRepos.safeName], `owner/repo` otherwise.
 */
@Composable
fun repoDisplayName(repoUrl: String, meta: RepoMeta?): String = when {
    ExtensionRepos.isOfficial(repoUrl) -> stringResource(R.string.ext_repo_official)
    else -> ExtensionRepos.safeName(meta?.name) ?: ExtensionRepos.name(repoUrl)
}

/**
 * What names where the extensions come from, see [extensionOrigin].
 *
 * @param repos The repos, null when unknown: an extension of another one comes from a removed repo.
 * @param installedBySource The installed extension of each source, by source id.
 */
data class ExtensionReposInfo(
    val repos: Set<String>? = null,
    val metas: Map<String, RepoMeta> = emptyMap(),
    val installedBySource: Map<Long, Extension.Installed> = emptyMap(),
)

val LocalExtensionRepos = compositionLocalOf { ExtensionReposInfo() }

/** The current [ExtensionReposInfo], with [ExtensionReposInfo.installedBySource] if [withSources]. */
@Composable
fun rememberExtensionReposInfo(withSources: Boolean = false): ExtensionReposInfo {
    val extensionManager = remember { Injekt.get<ExtensionManager>() }
    val sourcesPreferences by rememberDataStoreState(customPrefs = SourcesPreferences).value.collectAsState()
    val metas by extensionManager.repoMetasFlow.collectAsState()
    val installed by extensionManager.installedExtensionsFlow.collectAsState()
    val installedBySource = if (withSources) installed else emptyList()
    return remember(sourcesPreferences.extensionsRepos, metas, installedBySource) {
        ExtensionReposInfo(
            repos = sourcesPreferences.extensionsRepos,
            metas = metas,
            installedBySource = installedBySource
                .flatMap { extension -> extension.sources.map { it.id to extension } }
                .toMap(),
        )
    }
}

/**
 * Where an extension comes from, for the user: the repo [repoUrl] it comes from, see
 * [repoDisplayName], none when it's unknown.
 *
 * @param isManual Whether it was installed with adb in a debug build.
 */
@Composable
fun extensionOrigin(repoUrl: String?, isManual: Boolean = false): String? {
    val info = LocalExtensionRepos.current
    return when {
        isManual -> stringResource(R.string.ext_origin_manual)
        repoUrl == null -> null
        info.repos != null && repoUrl !in info.repos -> stringResource(R.string.ext_origin_no_repo)
        else -> repoDisplayName(repoUrl, info.metas[repoUrl])
    }
}

/** Where [extension] comes from, see [extensionOrigin]. */
@Composable
fun extensionOrigin(extension: Extension): String? = when (extension) {
    is Extension.Installed -> extensionOrigin(extension.repoUrl, extension.isManual)
    is Extension.Available -> extensionOrigin(extension.repoUrl)
}
