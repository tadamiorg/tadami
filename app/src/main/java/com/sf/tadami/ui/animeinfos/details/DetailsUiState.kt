package com.sf.tadami.ui.animeinfos.details

import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.ui.category.CategoryCheckbox
import com.sf.tadami.ui.components.data.EpisodeItem

data class DetailsUiState (
    val details : Anime? = null,
    val episodes : List<EpisodeItem> = emptyList(),
    val downloadedOnly : Boolean = false,
    val setCategoriesDialog : SetCategoriesDialogState? = null
)

/** [addToLibrary]: the anime joins the library once its categories are chosen */
data class SetCategoriesDialogState(
    val selection : List<CategoryCheckbox>,
    val addToLibrary : Boolean
)

enum class DetailsScreenItem {
    INFO_BOX,
    ACTION_ROW,
    DESCRIPTION_WITH_TAG,
    EPISODE_HEADER,
    EPISODE,
}