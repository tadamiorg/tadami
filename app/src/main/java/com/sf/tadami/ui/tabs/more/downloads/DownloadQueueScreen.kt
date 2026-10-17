package com.sf.tadami.ui.tabs.more.downloads

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.data.DropDownAction
import com.sf.tadami.ui.components.grid.EmptyScreen
import com.sf.tadami.ui.components.material.ExtendedFloatingActionButton
import com.sf.tadami.ui.components.topappbar.TadaTopAppBar
import com.sf.tadami.ui.components.widgets.ScrollbarLazyColumn
import com.sf.tadami.ui.utils.padding

/** The download queue, in the order it is downloaded, with its controls. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadQueueScreen(
    navController: NavHostController,
    viewModel: DownloadQueueViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TadaTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.label_download_queue),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                actions = if (uiState.queue.isEmpty()) emptyList() else listOf(
                    Action.DropDownVector(
                        title = R.string.stub_text,
                        icon = Icons.Outlined.MoreVert,
                        onClick = {},
                        items = listOf(
                            DropDownAction(
                                title = stringResource(R.string.download_queue_cancel_all),
                                onClick = viewModel::cancelAll,
                            ),
                            DropDownAction(
                                title = stringResource(R.string.download_queue_clear_errors),
                                onClick = viewModel::clearErrors,
                                enabled = uiState.hasErrors,
                            ),
                        ),
                    ),
                ),
            )
        },
        floatingActionButton = {
            when {
                uiState.isScheduled -> ExtendedFloatingActionButton(
                    text = { Text(text = stringResource(R.string.action_pause)) },
                    icon = { Icon(imageVector = Icons.Filled.Pause, contentDescription = null) },
                    onClick = viewModel::pause,
                )
                uiState.hasQueued -> ExtendedFloatingActionButton(
                    text = { Text(text = stringResource(R.string.action_resume)) },
                    icon = { Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null) },
                    onClick = viewModel::resume,
                )
            }
        },
    ) { contentPadding ->
        if (uiState.queue.isEmpty()) {
            EmptyScreen(
                modifier = Modifier.padding(contentPadding),
                message = stringResource(R.string.download_queue_empty),
            )
            return@Scaffold
        }
        ScrollbarLazyColumn(
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                // Room for the button over the last item
                bottom = contentPadding.calculateBottomPadding() + 88.dp,
            ),
        ) {
            // Its network constraint isn't met yet
            if (uiState.isScheduled && !uiState.isRunning && uiState.hasQueued) {
                item(key = "waiting") {
                    Text(
                        text = stringResource(
                            if (uiState.onlyOverWifi) R.string.download_queue_waiting_wifi
                            else R.string.download_queue_waiting_network
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = MaterialTheme.padding.medium,
                            vertical = MaterialTheme.padding.extraSmall,
                        ),
                    )
                }
            }
            items(uiState.queue, key = { it.episode.id }) { download ->
                DownloadQueueItem(
                    modifier = Modifier.animateItem(),
                    download = download,
                    isFirst = download == uiState.queue.first(),
                    isLast = download == uiState.queue.last(),
                    onMoveToTop = { viewModel.moveToTop(download) },
                    onMoveToBottom = { viewModel.moveToBottom(download) },
                    onRetry = { viewModel.retry(download) },
                    onCancel = { viewModel.cancel(download) },
                )
            }
        }
    }
}
