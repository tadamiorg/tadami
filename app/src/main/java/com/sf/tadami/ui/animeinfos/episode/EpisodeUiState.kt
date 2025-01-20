package com.sf.tadami.ui.animeinfos.episode

import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.source.model.Track

data class EpisodeUiState (
    val rawUrl : String? = null,
    val selectedSource : StreamSource? = null,
    val selectedSubtitleTrack: Track.SubtitleTrack? = null,
    val availableSources : List<StreamSource> = listOf(),
    val loadError : Boolean = false
)

