package com.sf.tadami.ui.tabs.browse.tabs.extensions.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.model.SourceIdConflict

/** Why [extension] can't be used and what fixes it, null when it can be. */
@Composable
fun loadIssueMessage(extension: Extension.Installed): String? {
    val issue = extension.loadIssue ?: return null
    return when (val conflict = extension.sourceIdConflict) {
        SourceIdConflict.Duplicate -> stringResource(R.string.ext_issue_source_id_duplicate_message)
        is SourceIdConflict.TakenBy -> stringResource(issue.messageRes, conflict.ownerName)
        null -> stringResource(issue.messageRes)
    }
}
