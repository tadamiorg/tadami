package com.sf.animescraper.ui.animeinfos.details

import com.sf.animescraper.domain.anime.Anime
import com.sf.animescraper.ui.components.data.EpisodeItem

data class DetailsUiState (
    val details : Anime? = null,
    val episodes : List<EpisodeItem> = emptyList()
)

enum class DetailsScreenItem {
    INFO_BOX,
    DESCRIPTION_WITH_TAG,
    EPISODE_HEADER,
    EPISODE,
}