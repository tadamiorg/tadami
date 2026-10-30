package com.sf.tadami.ui.components.globalSearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sf.tadami.R
import com.sf.tadami.ui.components.widgets.CloudflareVerificationLine
import com.sf.tadami.ui.utils.padding

@Composable
fun GlobalSearchLoadingResultItem(sourceBaseUrl: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.padding.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
        )
        CloudflareVerificationLine(sourceBaseUrl)
    }
}

@Composable
fun GlobalSearchErrorResultItem(
    message: String?,
    // Shown when a DNS blocking the source may cause the error
    onOpenDohSetting: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.extraSmall,
            )
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(imageVector = Icons.Outlined.Error, contentDescription = null)
        Spacer(Modifier.height(4.dp))
        Text(
            text = message ?: stringResource(R.string.unknown_error),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
        if (onOpenDohSetting != null) {
            Text(
                text = stringResource(R.string.doh_may_fix_error),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onOpenDohSetting) {
                Icon(
                    imageVector = Icons.Outlined.Dns,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(MaterialTheme.padding.extraSmall))
                Text(text = stringResource(R.string.action_use_doh))
            }
        }
    }
}