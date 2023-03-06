package com.sf.animescraper.ui.animeinfos.details

import com.sf.animescraper.network.scraping.dto.details.AnimeDetails
import com.sf.animescraper.network.scraping.dto.details.DetailsEpisode

data class DetailsUiState (
    val details : AnimeDetails? = null,
    val episodes : List<DetailsEpisode> = listOf()
)

enum class DetailsScreenItem {
    INFO_BOX,
    DESCRIPTION_WITH_TAG,
    EPISODE_HEADER,
    EPISODE,
}