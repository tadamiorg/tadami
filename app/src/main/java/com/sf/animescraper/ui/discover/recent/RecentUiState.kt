package com.sf.animescraper.ui.discover.recent

import com.sf.animescraper.network.scraping.dto.search.Anime

data class RecentUiState (
    var animeList : List<Anime> = listOf(),
    var hasNextPage : Boolean = true
)