package com.sf.animescraper.ui.discover.recent

import androidx.compose.runtime.Composable
import androidx.paging.compose.LazyPagingItems
import com.sf.animescraper.domain.anime.Anime
import com.sf.animescraper.ui.components.InfiniteAnimeGrid

@Composable
fun RecentComponent(
    animeList: LazyPagingItems<Anime>,
    onAnimeClicked: (anime:Anime) -> Unit,
) {
    InfiniteAnimeGrid(animeList = animeList, onAnimeCLicked = onAnimeClicked)
}





