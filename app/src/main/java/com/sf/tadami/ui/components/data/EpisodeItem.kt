package com.sf.tadami.ui.components.data

import com.sf.tadami.domain.episode.Episode

data class EpisodeItem(
    val episode : Episode,
    val selected : Boolean = false
)
