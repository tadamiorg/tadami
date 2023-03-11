package com.sf.animescraper.network.api.online

import com.sf.animescraper.network.api.model.SAnime

data class AnimesPage(val animes : List<SAnime>, val hasNextPage : Boolean)