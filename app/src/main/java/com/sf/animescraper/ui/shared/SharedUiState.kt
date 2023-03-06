package com.sf.animescraper.ui.shared

import com.sf.animescraper.network.scraping.AnimeSource
import com.sf.animescraper.network.scraping.dto.search.Anime

data class SharedUiState(
    val source: AnimeSource? = null,
    val selectedAnime: Anime? = null
)