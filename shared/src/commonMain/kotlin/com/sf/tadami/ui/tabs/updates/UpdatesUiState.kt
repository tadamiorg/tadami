package com.sf.tadami.ui.tabs.updates

import androidx.compose.runtime.Immutable
import com.sf.tadami.domain.updates.UpdatesWithRelations
import com.sf.tadami.ui.utils.insertSeparators
import com.sf.tadami.ui.utils.toDateKey
import java.util.Date

@Immutable
data class UpdatesUiState(
    val items: List<UpdatesItem>? = null,
){
    val selected = items?.filter { it.selected } ?: emptyList()
    val selectionMode = selected.isNotEmpty()

    fun getUiModel(): List<UpdatesUiModel> {
        return items
            ?.map { UpdatesUiModel.Item(it) }
            ?.insertSeparators { before, after ->
                val beforeDate = before?.item?.update?.dateFetch?.toDateKey() ?: Date(0)
                val afterDate = after?.item?.update?.dateFetch?.toDateKey() ?: Date(0)
                when {
                    beforeDate.time != afterDate.time && afterDate.time != 0L -> {
                        UpdatesUiModel.Header(afterDate)
                    }
                    // Return null to avoid adding a separator between two items.
                    else -> null
                }
            } ?: emptyList()
    }
}

@Immutable
data class UpdatesItem(
    val update: UpdatesWithRelations,
    val selected: Boolean = false,
)

sealed interface UpdatesUiModel {
    data class Header(val date: Date) : UpdatesUiModel
    data class Item(val item: UpdatesItem) : UpdatesUiModel
}
