package com.sf.tadami.ui.tabs.browse.tabs.extensions.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sf.tadami.R
import com.sf.tadami.extension.model.LoadResult
import com.sf.tadami.ui.utils.padding

@get:StringRes
val LoadResult.Reason.textRes: Int
    get() = when (this) {
        LoadResult.Reason.NOT_FOUND -> R.string.ext_failed_reason_not_found
        LoadResult.Reason.MALFORMED -> R.string.ext_failed_reason_malformed
        LoadResult.Reason.INCOMPATIBLE_API -> R.string.ext_failed_reason_incompatible
        LoadResult.Reason.CLASSLOAD_FAILED -> R.string.ext_failed_reason_classload
        LoadResult.Reason.TIMED_OUT -> R.string.ext_failed_reason_timed_out
        LoadResult.Reason.QUARANTINED -> R.string.ext_failed_reason_quarantined
    }

/**
 * An extension that couldn't be loaded. Without this the extension would simply be missing from
 * the list with no explanation, which is indistinguishable from "not installed".
 */
@Composable
fun FailedExtensionItem(
    error: LoadResult.Error,
    onRetry: (String) -> Unit,
    onUninstall: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
            Column(modifier = Modifier.padding(start = MaterialTheme.padding.medium)) {
                Text(
                    text = error.extName ?: error.pkgName,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(error.reason.textRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(modifier = Modifier.padding(top = 4.dp)) {
            // Retrying a NOT_FOUND package can't succeed — there's nothing left to load.
            if (error.reason != LoadResult.Reason.NOT_FOUND) {
                TextButton(onClick = { onRetry(error.pkgName) }) {
                    Text(text = stringResource(R.string.ext_failed_retry))
                }
            }
            TextButton(onClick = { onUninstall(error.pkgName) }) {
                Text(
                    text = stringResource(R.string.ext_uninstall),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
