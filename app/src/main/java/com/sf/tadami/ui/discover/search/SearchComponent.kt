package com.sf.tadami.ui.discover.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.ui.components.InfiniteAnimeGrid

@Composable
fun SearchComponent(
    modifier: Modifier = Modifier,
    animeList: LazyPagingItems<Anime>,
    onAnimeClicked: (anime: Anime) -> Unit,
    fabPadding: PaddingValues
) {
    InfiniteAnimeGrid(
        modifier = modifier,
        contentPadding = fabPadding,
        animeList = animeList,
        onAnimeClicked = onAnimeClicked,
    )
}