package com.sf.tadami.ui.tabs.history

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
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
                    stringResource(Res.string.pager_no_results)
                } else {
                    stringResource(Res.string.information_no_recent_anime)
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