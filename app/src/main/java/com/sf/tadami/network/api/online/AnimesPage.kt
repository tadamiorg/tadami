package com.sf.tadami.network.api.online

import com.sf.tadami.network.api.model.SAnime

data class AnimesPage(val animes : List<SAnime>, val hasNextPage : Boolean)