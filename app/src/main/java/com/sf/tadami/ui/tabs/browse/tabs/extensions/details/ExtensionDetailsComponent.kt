package com.sf.tadami.ui.tabs.browse.tabs.extensions.details

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.sf.tadami.R
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.ui.components.banners.WarningBanner
import com.sf.tadami.ui.components.widgets.ScrollbarLazyColumn

@Composable
fun ExtensionDetailsComponent(
    contentPadding : PaddingValues,
    onClickUninstall: () -> Unit,
    extension : Extension.Installed
) {
    val context = LocalContext.current
    ScrollbarLazyColumn(
        contentPadding = contentPadding,
    ) {
        if (extension.isObsolete) {
            item {
                WarningBanner(R.string.ext_obsolete)
            }
        }

        item {
            ExtensionDetailsHeader(
                extension = extension,
                onClickUninstall = onClickUninstall,
                onClickAppInfo = {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", extension.pkgName, null)
                        context.startActivity(this)
                    }
                    Unit
                }.takeIf { extension.isShared },
            )
        }
    }
}