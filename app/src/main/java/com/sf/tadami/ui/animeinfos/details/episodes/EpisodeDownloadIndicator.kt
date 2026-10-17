package com.sf.tadami.ui.animeinfos.details.episodes

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sf.tadami.R
import com.sf.tadami.data.download.model.Download
import com.sf.tadami.data.download.model.EpisodeDownloadState

enum class EpisodeDownloadAction {
    START,
    CANCEL,
    DELETE,
    RETRY,
}

/** The download state of an episode, and its actions: download, cancel, delete, retry. */
@Composable
fun EpisodeDownloadIndicator(
    download: EpisodeDownloadState,
    onAction: (EpisodeDownloadAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpened by remember { mutableStateOf(false) }
    val iconColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        IconButton(
            onClick = {
                if (download.state == Download.State.NOT_DOWNLOADED) {
                    onAction(EpisodeDownloadAction.START)
                } else {
                    menuOpened = true
                }
            },
        ) {
            when (download.state) {
                Download.State.NOT_DOWNLOADED -> Icon(
                    imageVector = Icons.Outlined.ArrowCircleDown,
                    contentDescription = stringResource(R.string.download),
                    tint = iconColor,
                )
                Download.State.QUEUED -> {
                    val description = stringResource(R.string.episode_download_queued)
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(IndicatorSize)
                            .semantics { contentDescription = description },
                        color = iconColor,
                        strokeWidth = IndicatorStroke,
                    )
                }
                Download.State.DOWNLOADING -> {
                    val description = stringResource(R.string.episode_download_progress, download.progress)
                    Box(
                        modifier = Modifier.semantics { contentDescription = description },
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            progress = { download.progress / 100f },
                            modifier = Modifier.size(IndicatorSize),
                            color = iconColor,
                            strokeWidth = IndicatorStroke,
                            trackColor = iconColor.copy(alpha = 0.2f),
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDownward,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = iconColor,
                        )
                    }
                }
                Download.State.DOWNLOADED -> Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.episode_downloaded),
                    tint = iconColor,
                )
                Download.State.ERROR -> Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = stringResource(R.string.episode_download_error),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        DropdownMenu(
            expanded = menuOpened,
            onDismissRequest = { menuOpened = false },
        ) {
            val select = { action: EpisodeDownloadAction ->
                menuOpened = false
                onAction(action)
            }
            when (download.state) {
                Download.State.QUEUED, Download.State.DOWNLOADING ->
                    MenuItem(R.string.episode_download_cancel) { select(EpisodeDownloadAction.CANCEL) }
                Download.State.DOWNLOADED ->
                    MenuItem(R.string.episode_download_delete) { select(EpisodeDownloadAction.DELETE) }
                Download.State.ERROR -> {
                    MenuItem(R.string.episode_download_retry) { select(EpisodeDownloadAction.RETRY) }
                    MenuItem(R.string.episode_download_cancel) { select(EpisodeDownloadAction.CANCEL) }
                }
                Download.State.NOT_DOWNLOADED -> Unit
            }
        }
    }
}

@Composable
private fun MenuItem(@StringRes text: Int, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = stringResource(text)) },
        onClick = onClick,
    )
}

private val IndicatorSize = 22.dp
private val IndicatorStroke = 2.dp
