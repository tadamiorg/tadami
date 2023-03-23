package com.sf.tadami.ui.components.data

import com.sf.tadami.domain.anime.FavoriteAnime

data class FavoriteItem(
    val anime : FavoriteAnime,
    val selected : Boolean
)
