package com.sf.tadami.ui.components.data

import com.sf.tadami.domain.anime.LibraryAnime

data class LibraryItem(
    val anime : LibraryAnime,
    val selected : Boolean
)
