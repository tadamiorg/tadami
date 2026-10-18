package com.sf.tadami.ui.tabs.browse.tabs.extensions.details

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.sf.tadami.R
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.model.ExtensionLoadIssue
import com.sf.tadami.ui.components.banners.WarningBanner
import com.sf.tadami.ui.components.widgets.ScrollbarLazyColumn
import com.sf.tadami.ui.tabs.browse.tabs.extensions.components.loadIssueMessage

@Composable
fun ExtensionDetailsComponent(
    contentPadding : PaddingValues,
    onClickUninstall: () -> Unit,
    onClickTrust: () -> Unit,
    onClickRetry: () -> Unit,
    extension : Extension.Installed,
    isReloading: Boolean = false,
) {
    ScrollbarLazyColumn(
        contentPadding = contentPadding,
    ) {
        if (extension.loadIssue != null) {
            item {
                WarningBanner(loadIssueMessage(extension).orEmpty())
            }
        }

        if (extension.isObsolete) {
            item {
                WarningBanner(R.string.ext_obsolete)
            }
        }

        item {
            ExtensionDetailsHeader(
                extension = extension,
                onClickUninstall = onClickUninstall,
                // Not while it's loaded again, once trusted
                onClickTrust = onClickTrust.takeIf { extension.loadIssue == ExtensionLoadIssue.UNTRUSTED && !isReloading },
                onClickRetry = onClickRetry.takeIf { extension.loadIssue == ExtensionLoadIssue.QUARANTINED },
            )
        }
    }
}