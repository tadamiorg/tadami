package com.sf.tadami.ui.tabs.history

import com.sf.tadami.domain.history.HistoryWithRelations
import java.util.Date

data class HistoryUiState(
    val searchQuery: String? = null,
    val list: List<HistoryUiModel>? = null,
    val historyToDelete : HistoryWithRelations? = null
)

sealed class HistoryUiModel {
    data class Header(val date: Date) : HistoryUiModel()
    data class Item(val item: HistoryWithRelations) : HistoryUiModel()
}