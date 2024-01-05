package com.sf.tadami.ui.tabs.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.animeInfos.AnimeInfosRoutes
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.topappbar.search.SearchTopAppBar
import com.sf.tadami.ui.tabs.history.dialogs.DeleteAllHistoryDialog
import com.sf.tadami.ui.tabs.history.dialogs.DeleteHistoryDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    historyViewModel: HistoryViewModel = viewModel()
) {
    val uiState by historyViewModel.uiState.collectAsState()
    var searchValue by rememberSaveable { mutableStateOf("") }
    var searchOpened by rememberSaveable { mutableStateOf(false) }
    var historyDialogOpened by rememberSaveable { mutableStateOf<HistoryDialogs?>(null) }
    val snackBarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val actions = remember {
        listOf(
            Action.Drawable(
                title = R.string.stub_text,
                icon = R.drawable.ic_delete_sweep,
                enabled = true,
                onClick = {
                    historyDialogOpened = HistoryDialogs.DeleteAll
                }),
            Action.CastButton()
        )
    }

    when (historyDialogOpened) {
        is HistoryDialogs.DeleteSingle -> {
            DeleteHistoryDialog(
                onDismissRequest = {
                    historyViewModel.setHistoryToDelete(null)
                    historyDialogOpened = null
                },
                onConfirm = { all ->
                    if (uiState.historyToDelete != null) {
                        if (all) {
                            historyViewModel.removeAllFromHistory(uiState.historyToDelete!!.animeId)
                        } else {
                            historyViewModel.removeFromHistory(uiState.historyToDelete!!)
                        }
                    }
                }
            )
        }

        is HistoryDialogs.DeleteAll -> {
            DeleteAllHistoryDialog(
                onDismissRequest = { historyDialogOpened = null },
                onConfirm = {
                    historyViewModel.removeAllHistory {
                        snackBarHostState.currentSnackbarData?.dismiss()
                        snackBarHostState.showSnackbar(
                            message = context.getString(R.string.clear_history_completed)
                        )
                    }
                }
            )
        }

        else -> {}
    }



    Scaffold(
        modifier = modifier,
        topBar = {
            SearchTopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.label_history))
                },
                searchOpened = searchOpened,
                onSearchOpen = {
                    searchOpened = true
                },
                onSearchChange = {
                    searchValue = it
                    historyViewModel.updateSearchQuery(it)
                },
                onSearchCancel = {
                    searchValue = ""
                    searchOpened = false
                    historyViewModel.updateSearchQuery(null)
                },
                searchValue = searchValue,
                actions = actions
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            HistoryComponent(
                uiState = uiState,
                onClickCover = { historyItem ->
                    navController.navigate("${AnimeInfosRoutes.DETAILS}/${historyItem.source}/${historyItem.animeId}")
                },
                onClickResume = { historyItem ->
                    historyViewModel.getNextEpisodeForAnime(
                        animeId = historyItem.animeId,
                        episodeId = historyItem.episodeId
                    ) { nextEpisode ->
                        if (nextEpisode != null) {
                            navController.navigate("${AnimeInfosRoutes.EPISODE}/${historyItem.source}/${nextEpisode.id}")
                        } else {
                            snackBarHostState.currentSnackbarData?.dismiss()
                            snackBarHostState.showSnackbar(
                                message = context.getString(R.string.next_episode_not_found)
                            )
                        }
                    }
                },
                onClickDelete = {
                    historyViewModel.setHistoryToDelete(it)
                    historyDialogOpened = HistoryDialogs.DeleteSingle
                }
            )
        }
    }
}

private sealed class HistoryDialogs() {
    data object DeleteAll : HistoryDialogs()

    data object DeleteSingle : HistoryDialogs()
}