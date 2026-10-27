package com.sf.tadami.ui.discover.migrate

import com.sf.tadami.source.AnimeCatalogueSource
import com.sf.tadami.ui.components.globalSearch.GlobalSearchItemResult

data class MigrateSearchUiState(
    val fromSourceId: Long? = null,
    val items: Map<AnimeCatalogueSource, GlobalSearchItemResult> = emptyMap(),
) {
    val progress: Int = items.count { it.value !is GlobalSearchItemResult.Loading }
    val total: Int = items.size
}