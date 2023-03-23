package com.sf.tadami.ui.discover.recent

import androidx.compose.runtime.Composable
import androidx.paging.compose.LazyPagingItems
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.ui.components.InfiniteAnimeGrid

@Composable
fun RecentComponent(
    animeList: LazyPagingItems<Anime>,
    onAnimeClicked: (anime:Anime) -> Unit,
) {
    InfiniteAnimeGrid(
        animeList = animeList,
        onAnimeClicked = onAnimeClicked
    )
}





