package com.sf.tadami.ui.tabs.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sf.tadami.domain.history.HistoryWithRelations
import com.sf.tadami.ui.components.widgets.FastScrollLazyColumn
import com.sf.tadami.ui.tabs.history.components.DateHeader
import com.sf.tadami.ui.tabs.history.components.HistoryItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryContent(
    history: List<HistoryUiModel>,
    onClickCover: (HistoryWithRelations) -> Unit,
    onClickResume: (HistoryWithRelations) -> Unit,
    onClickDelete: (HistoryWithRelations) -> Unit,
) {
    FastScrollLazyColumn{
        items(
            items = history,
            key = { "history-${it.hashCode()}" },
            contentType = {
                when (it) {
                    is HistoryUiModel.Header -> "header"
                    is HistoryUiModel.Item -> "item"
                }
            },
        ) { item ->
            when (item) {
                is HistoryUiModel.Header -> {
                    DateHeader(
                        modifier = Modifier.animateItemPlacement(),
                        date = item.date,
                    )
                }
                is HistoryUiModel.Item -> {
                    val value = item.item
                    HistoryItem(
                        modifier = Modifier.animateItemPlacement(),
                        history = value,
                        onClickCover = { onClickCover(value) },
                        onClickResume = { onClickResume(value) },
                        onClickDelete = { onClickDelete(value) },
                    )
                }
            }
        }
    }
}
