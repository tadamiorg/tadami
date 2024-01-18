package com.sf.tadami.ui.tabs.updates.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import com.sf.tadami.ui.tabs.updates.UpdatesItem
import com.sf.tadami.ui.tabs.updates.UpdatesUiModel
import com.sf.tadami.ui.utils.formatMinSec

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.updatesUiItems(
    uiModels: List<UpdatesUiModel>,
    selectionMode: Boolean,
    onUpdateSelected: (UpdatesItem, Boolean) -> Unit,
    onClickCover: (UpdatesItem) -> Unit,
    onClickUpdate: (UpdatesItem) -> Unit
) {
    items(
        items = uiModels,
        contentType = {
            when (it) {
                is UpdatesUiModel.Header -> "header"
                is UpdatesUiModel.Item -> "item"
            }
        },
        key = {
            when (it) {
                is UpdatesUiModel.Header -> "updatesHeader-${it.hashCode()}"
                is UpdatesUiModel.Item -> "updates-${it.item.update.animeId}-${it.item.update.episodeId}"
            }
        },
    ) { item ->
        when (item) {
            is UpdatesUiModel.Header -> {
                ListGroupHeader(
                    modifier = Modifier.animateItemPlacement(),
                    text = relativeDateText(item.date),
                )
            }
            is UpdatesUiModel.Item -> {
                val updatesItem = item.item
                UpdatesUiItem(
                    modifier = Modifier.animateItemPlacement(),
                    update = updatesItem.update,
                    selected = updatesItem.selected,
                    seenProgress = updatesItem.update.timeSeen
                        .takeIf { !updatesItem.update.seen && it > 0L }
                        ?.let {
                            "${it.formatMinSec()}/${updatesItem.update.totalTime.formatMinSec()}"
                        },

                    onLongClick = {
                        onUpdateSelected(updatesItem, !updatesItem.selected)
                    },
                    onClick = {
                        when {
                            selectionMode -> onUpdateSelected(updatesItem, !updatesItem.selected)
                            else -> onClickUpdate(updatesItem)
                        }
                    },
                    onClickCover = { onClickCover(updatesItem) }.takeIf { !selectionMode },
                )
            }
        }
    }
}