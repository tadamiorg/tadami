package com.sf.tadami.ui.discover.recent

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.paging.compose.LazyPagingItems
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.ui.components.grid.InfiniteAnimeGrid

@Composable
fun RecentComponent(
    animeList: LazyPagingItems<Anime>,
    snackbarHostState : SnackbarHostState,
    onAnimeClicked: (anime:Anime) -> Unit,
) {
    InfiniteAnimeGrid(
        animeList = animeList,
        snackbarHostState = snackbarHostState,
        onAnimeClicked = onAnimeClicked
    )
}





