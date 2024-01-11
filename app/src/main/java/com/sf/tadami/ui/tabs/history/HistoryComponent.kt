package com.sf.tadami.ui.tabs.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.domain.history.HistoryWithRelations
import com.sf.tadami.ui.components.grid.EmptyScreen
import com.sf.tadami.ui.components.widgets.ContentLoader

@Composable
fun HistoryComponent(
    uiState: HistoryUiState,
    onClickCover: (HistoryWithRelations) -> Unit,
    onClickResume: (HistoryWithRelations) -> Unit,
    onClickDelete: (HistoryWithRelations) -> Unit,
) {

    uiState.list.let{
        ContentLoader(isLoading = it == null){
            if(it!!.isEmpty()){
                val msg = if (!uiState.searchQuery.isNullOrEmpty()) {
                    stringResource(id = R.string.pager_no_results)
                } else {
                    stringResource(id = R.string.information_no_recent_anime)
                }
                EmptyScreen(
                    message = msg,
                )
            }
            else{
                HistoryContent(
                    history = it,
                    onClickCover = { historyItem -> onClickCover(historyItem) },
                    onClickResume = { historyItem -> onClickResume(historyItem)},
                    onClickDelete = onClickDelete
                )
            }
        }
    }




}