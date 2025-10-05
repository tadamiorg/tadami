package com.sf.tadami.ui.tabs.updates

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import com.sf.tadami.R
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.ui.components.grid.EmptyScreen
import com.sf.tadami.ui.components.widgets.ContentLoader
import com.sf.tadami.ui.components.widgets.FastScrollLazyColumn
import com.sf.tadami.ui.components.widgets.PullRefresh
import com.sf.tadami.ui.tabs.updates.components.updatesUiItems
import com.sf.tadami.ui.utils.padding
import com.sf.tadami.ui.utils.relativeTimeSpanString
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UpdatesComponent(
    uiState: UpdatesUiState,
    onUpdateLibrary : () -> Boolean,
    isRefreshing : Boolean,
    snackBarHostState : SnackbarHostState,
    onUpdateSelected: (UpdatesItem, Boolean) -> Unit,
    onOpenEpisode: (UpdatesItem) -> Unit,
    onClickCover: (UpdatesItem) -> Unit,
) {
    val libraryPreferences by rememberDataStoreState(customPrefs = LibraryPreferences).value.collectAsState()
    uiState.items.let{
        ContentLoader(isLoading = it == null){
            if(it!!.isEmpty()){
                EmptyScreen(
                    message =  stringResource(id = R.string.information_no_recent_episodes),
                )
            }
            else{
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                PullRefresh(
                    refreshing = isRefreshing,
                    onRefresh = {
                        val started = onUpdateLibrary()
                        val msgRes = if (started) context.getString(R.string.update_starting) else context.getString(R.string.update_running)
                        scope.launch {
                            snackBarHostState.currentSnackbarData?.dismiss()
                            snackBarHostState.showSnackbar(msgRes)
                        }
                    },
                    enabled = !uiState.selectionMode,
                ) {
                    FastScrollLazyColumn() {
                        item(key = "updates-lastUpdated") {
                            Box(
                                modifier = Modifier
                                    .animateItem()
                                    .padding(
                                        horizontal = MaterialTheme.padding.medium,
                                        vertical = MaterialTheme.padding.small
                                    ),
                            ) {
                                Text(
                                    text = stringResource(R.string.updates_last_update_info, relativeTimeSpanString(libraryPreferences.lastUpdatedTimestamp)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontStyle = FontStyle.Italic,
                                )
                            }
                        }

                        updatesUiItems(
                            uiModels = uiState.getUiModel(),
                            selectionMode = uiState.selectionMode,
                            onUpdateSelected = onUpdateSelected,
                            onClickCover = onClickCover,
                            onClickUpdate = onOpenEpisode,
                        )
                    }
                }
            }
        }
    }

}