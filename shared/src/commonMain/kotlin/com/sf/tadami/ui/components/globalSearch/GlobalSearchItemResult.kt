package com.sf.tadami.ui.components.globalSearch

import com.sf.tadami.domain.anime.Anime

sealed class GlobalSearchItemResult {
    object Loading : GlobalSearchItemResult()

    data class Error(
        val throwable: Throwable,
    ) : GlobalSearchItemResult()

    data class Success(
        val result: List<Anime>,
    ) : GlobalSearchItemResult() {
        val isEmpty: Boolean
            get() = result.isEmpty()
    }
}