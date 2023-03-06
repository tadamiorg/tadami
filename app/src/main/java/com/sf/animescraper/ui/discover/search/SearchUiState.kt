package com.sf.animescraper.ui.discover.search

import com.sf.animescraper.network.scraping.dto.search.Anime

data class SearchUiState(
    var animeList: List<Anime> = listOf(),
    var hasNextPage: Boolean = true
)