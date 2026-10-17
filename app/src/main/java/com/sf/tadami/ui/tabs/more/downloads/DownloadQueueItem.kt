package com.sf.tadami.ui.tabs.more.downloads

import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.sf.tadami.R
import com.sf.tadami.data.download.model.Download
import com.sf.tadami.ui.utils.padding

/** One queued episode: its anime, its state and progress, and its actions. */
@Composable
fun DownloadQueueItem(
    download: Download,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveToTop: () -> Unit,
    onMoveToBottom: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status by download.statusFlow.collectAsState()
    val progress by download.progressFlow.collectAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = MaterialTheme.padding.medium, top = MaterialTheme.padding.extraSmall, bottom = MaterialTheme.padding.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = download.anime.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = download.episode.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = statusText(download, status, progress),
                style = MaterialTheme.typography.bodySmall,
                color = if (status == Download.State.ERROR) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (status == Download.State.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.padding.tiny),
                )
            }
        }
        DownloadQueueItemMenu(
            status = status,
            isFirst = isFirst,
            isLast = isLast,
            onMoveToTop = onMoveToTop,
            onMoveToBottom = onMoveToBottom,
            onRetry = onRetry,
            onCancel = onCancel,
        )
    }
}

@Composable
private fun statusText(download: Download, status: Download.State, progress: Int): String {
    return when (status) {
        Download.State.ERROR -> download.error?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.episode_download_error)
        Download.State.DOWNLOADING -> {
            val video = download.videoProgress
            when {
                video == null -> stringResource(R.string.download_queue_progress, progress)
                video.hls && video.total > 0 -> stringResource(
                    R.string.download_queue_progress_segments,
                    progress,
                    video.segments,
                    video.total,
                )
                !video.hls && video.total > 0 -> stringResource(
                    R.string.download_queue_progress_size,
                    progress,
                    Formatter.formatShortFileSize(LocalContext.current, video.bytes),
                    Formatter.formatShortFileSize(LocalContext.current, video.total),
                )
                else -> stringResource(R.string.download_queue_progress, progress)
            }
        }
        else -> stringResource(R.string.download_queue_waiting)
    }
}

@Composable
private fun DownloadQueueItemMenu(
    status: Download.State,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveToTop: () -> Unit,
    onMoveToBottom: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    var menuOpened by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuOpened = true }) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = null,
            )
        }
        DropdownMenu(
            expanded = menuOpened,
            onDismissRequest = { menuOpened = false },
        ) {
            val select = { action: () -> Unit ->
                menuOpened = false
                action()
            }
            MenuItem(R.string.download_queue_move_to_top, enabled = !isFirst) { select(onMoveToTop) }
            MenuItem(R.string.download_queue_move_to_bottom, enabled = !isLast) { select(onMoveToBottom) }
            if (status == Download.State.ERROR) {
                MenuItem(R.string.episode_download_retry) { select(onRetry) }
            }
            MenuItem(R.string.episode_download_cancel) { select(onCancel) }
        }
    }
}

@Composable
private fun MenuItem(@StringRes text: Int, enabled: Boolean = true, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = stringResource(text)) },
        onClick = onClick,
        enabled = enabled,
    )
}
