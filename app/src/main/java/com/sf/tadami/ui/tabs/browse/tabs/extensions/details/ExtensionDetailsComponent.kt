package com.sf.tadami.ui.tabs.browse.tabs.extensions.details

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.sf.tadami.R
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.model.ExtensionLoadIssue
import com.sf.tadami.ui.components.banners.WarningBanner
import com.sf.tadami.ui.components.widgets.ScrollbarLazyColumn

@Composable
fun ExtensionDetailsComponent(
    contentPadding : PaddingValues,
    onClickUninstall: () -> Unit,
    onClickTrust: () -> Unit,
    extension : Extension.Installed
) {
    ScrollbarLazyColumn(
        contentPadding = contentPadding,
    ) {
        extension.loadIssue?.let { issue ->
            item {
                WarningBanner(issue.messageRes)
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
                onClickTrust = onClickTrust.takeIf { extension.loadIssue == ExtensionLoadIssue.UNTRUSTED },
            )
        }
    }
}