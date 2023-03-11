package com.sf.animescraper.ui.animeinfos.episode

import com.sf.animescraper.network.api.model.StreamSource

data class EpisodeUiState (
    val rawUrl : String? = null,
    val selectedSource : StreamSource? = null,
    val availableSources : List<StreamSource> = listOf()
)

