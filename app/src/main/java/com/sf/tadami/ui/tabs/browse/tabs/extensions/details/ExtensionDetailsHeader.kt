package com.sf.tadami.ui.tabs.browse.tabs.extensions.details

import android.util.DisplayMetrics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sf.tadami.R
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.ui.tabs.browse.tabs.extensions.components.ExtensionIcon
import com.sf.tadami.ui.utils.padding

@Composable
fun ExtensionDetailsHeader(
    extension: Extension,
    onClickUninstall: () -> Unit,
    onClickAppInfo: (() -> Unit)?,
) {
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MaterialTheme.padding.medium,
                    end = MaterialTheme.padding.medium,
                    top = MaterialTheme.padding.medium,
                    bottom = MaterialTheme.padding.small,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ExtensionIcon(
                modifier = Modifier
                    .size(112.dp),
                extension = extension,
                density = DisplayMetrics.DENSITY_XXXHIGH,
            )

            Text(
                text = extension.name,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )

            val strippedPkgName = extension.pkgName.substringAfter("com.sf.tadami.extension.")

            Text(
                text = strippedPkgName,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.padding.extraLarge,
                    vertical = MaterialTheme.padding.small,
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InfoText(
                modifier = Modifier.weight(1f),
                primaryText = extension.versionName,
                secondaryText = stringResource(R.string.ext_info_version),
            )

            InfoDivider()

            InfoText(
                modifier = Modifier.weight(1f),
                primaryText = stringResource(id = extension.lang?.getRes() ?: R.string.language_unknown),
                secondaryText = stringResource(R.string.ext_info_language),
            )
        }

        Column(
            modifier = Modifier.padding(
                start = MaterialTheme.padding.medium,
                end = MaterialTheme.padding.medium,
                top = MaterialTheme.padding.small,
                bottom = MaterialTheme.padding.medium,
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            if (onClickAppInfo != null) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClickAppInfo,
                ) {
                    Text(
                        text = stringResource(R.string.ext_app_info),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onClickUninstall,
            ) {
                Text(stringResource(R.string.ext_uninstall))
            }
        }
    }
}