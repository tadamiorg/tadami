package com.sf.animescraper.ui.discover.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import com.sf.animescraper.domain.anime.Anime
import com.sf.animescraper.ui.components.InfiniteAnimeGrid

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