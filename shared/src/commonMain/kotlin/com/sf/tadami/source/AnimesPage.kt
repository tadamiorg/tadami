package com.sf.tadami.source

import com.sf.tadami.source.model.SAnime

data class AnimesPage(val animes : List<SAnime>, val hasNextPage : Boolean)