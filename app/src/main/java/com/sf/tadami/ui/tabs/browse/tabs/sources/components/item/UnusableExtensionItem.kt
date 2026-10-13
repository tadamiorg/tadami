package com.sf.tadami.ui.tabs.browse.tabs.sources.components.item

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sf.tadami.R
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.ui.tabs.browse.tabs.extensions.components.ExtensionIcon
import com.sf.tadami.ui.tabs.browse.tabs.extensions.components.item.BaseBrowseItem
import com.sf.tadami.ui.utils.padding
import com.sf.tadami.ui.utils.secondaryItemAlpha

/**
 * An installed extension that can't be used ([Extension.Installed.loadIssue]), in place of its
 * sources: its buttons are there but disabled, and the reason is shown. Clicking it opens the
 * extension, where it can be uninstalled, or trusted when it's untrusted.
 */
@Composable
fun UnusableExtensionItem(
    extension: Extension.Installed,
    onClickItem: (Extension.Installed) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseBrowseItem(
        modifier = modifier,
        onClickItem = { onClickItem(extension) },
        onLongClickItem = { onClickItem(extension) },
        icon = {
            ExtensionIcon(
                extension = extension,
                modifier = Modifier
                    .height(40.dp)
                    .aspectRatio(1f)
                    .alpha(DISABLED_ALPHA),
            )
        },
        action = {
            TextButton(onClick = {}, enabled = false) {
                Text(text = stringResource(R.string.anime_sources_screen_recents_btn))
            }
            IconButton(onClick = {}, enabled = false) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_vertical_settings),
                    contentDescription = null,
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = MaterialTheme.padding.medium)
                .weight(1f),
        ) {
            Text(
                modifier = Modifier.secondaryItemAlpha(),
                text = extension.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            extension.loadIssue?.let { issue ->
                Text(
                    text = stringResource(issue.messageRes),
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private const val DISABLED_ALPHA = .38f
