package com.sf.animescraper.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.sf.animescraper.domain.anime.Anime
import com.sf.animescraper.ui.utils.rememberLazyGridState

@Composable
fun InfiniteAnimeGrid(
    modifier: Modifier = Modifier,
    animeList: LazyPagingItems<Anime>,
    onAnimeCLicked: (anime: Anime) -> Unit,
    contentPadding : PaddingValues = PaddingValues(0.dp),
) {

    val lazyGridState = animeList.rememberLazyGridState()

    AnimeGrid(
        modifier = modifier,
        contentPadding = contentPadding,
        animeList = animeList,
        onAnimeCLicked = onAnimeCLicked,
        lazyGridState = lazyGridState
    )

}