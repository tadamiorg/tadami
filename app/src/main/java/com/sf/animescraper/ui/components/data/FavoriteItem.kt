package com.sf.animescraper.ui.components.data

import com.sf.animescraper.domain.anime.FavoriteAnime

data class FavoriteItem(
    val anime : FavoriteAnime,
    val selected : Boolean
)
