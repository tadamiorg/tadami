package com.sf.tadami.ui.tabs.browse.tabs.extensions.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.extension.api.ExtensionRepos
import com.sf.tadami.extension.api.RepoMeta

/** The name of the repo [repoUrl] for the user: the name its [meta] gives, `owner/repo` otherwise. */
@Composable
fun repoDisplayName(repoUrl: String, meta: RepoMeta?): String = when {
    ExtensionRepos.isOfficial(repoUrl) -> stringResource(R.string.ext_repo_official)
    else -> meta?.name ?: ExtensionRepos.name(repoUrl)
}
