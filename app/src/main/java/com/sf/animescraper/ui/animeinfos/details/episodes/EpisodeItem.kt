package com.sf.animescraper.ui.animeinfos.details.episodes

import com.sf.animescraper.domain.episode.Episode

data class EpisodeItem(
    val episode : Episode,
    val selected : Boolean = false
)
