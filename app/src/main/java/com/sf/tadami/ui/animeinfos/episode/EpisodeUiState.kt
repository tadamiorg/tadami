package com.sf.tadami.ui.animeinfos.episode

import com.sf.tadami.source.model.StreamSource

data class EpisodeUiState (
    val rawUrl : String? = null,
    val selectedSource : StreamSource? = null,
    val availableSources : List<StreamSource> = listOf(),
    val loadError : Boolean = false
)

