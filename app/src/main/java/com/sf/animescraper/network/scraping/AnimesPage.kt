package com.sf.animescraper.network.scraping

import com.sf.animescraper.network.scraping.dto.search.Anime

data class AnimesPage(val animes : List<Anime>, val hasNextPage : Boolean)