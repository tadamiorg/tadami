package com.sf.tadami.source.online

import com.sf.tadami.source.model.SAnime

data class AnimesPage(val animes : List<SAnime>, val hasNextPage : Boolean)