package com.sf.tadami.ui.animeinfos.details

import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.ui.components.data.EpisodeItem

data class DetailsUiState (
    val details : Anime? = null,
    val episodes : List<EpisodeItem> = emptyList()
)

enum class DetailsScreenItem {
    INFO_BOX,
    ACTION_ROW,
    DESCRIPTION_WITH_TAG,
    EPISODE_HEADER,
    EPISODE,
}