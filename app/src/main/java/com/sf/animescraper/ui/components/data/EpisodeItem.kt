package com.sf.animescraper.ui.components.data

import com.sf.animescraper.domain.episode.Episode

data class EpisodeItem(
    val episode : Episode,
    val selected : Boolean = false
)
