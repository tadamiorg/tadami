package com.sf.tadami.ui.tabs.browse.tabs.extensions.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.model.SourceIdConflict

/** Why [extension] can't be used and what fixes it, null when it can be. */
@Composable
fun loadIssueMessage(extension: Extension.Installed): String? {
    val issue = extension.loadIssue ?: return null
    return when (val conflict = extension.sourceIdConflict) {
        SourceIdConflict.Duplicate -> stringResource(Res.string.ext_issue_source_id_duplicate_message)
        SourceIdConflict.Reserved -> stringResource(Res.string.ext_issue_source_id_reserved_message)
        is SourceIdConflict.TakenBy -> stringResource(issue.messageRes, conflict.ownerName)
        null -> stringResource(issue.messageRes)
    }
}
