package com.sf.animescraper.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sf.animescraper.network.scraping.dto.search.Anime
import com.sf.animescraper.ui.base.widgets.ContentLoader

@Composable
fun AnimeGrid(
    modifier: Modifier = Modifier,
    animeList: List<Anime>,
    onAnimeCLicked: (anime: Anime) -> Unit,
    lazyGridState: LazyGridState = rememberLazyGridState(),
    contentPadding : PaddingValues,
    isLoading: Boolean = false
) {
    ContentLoader(
        modifier = modifier,
        isLoading = isLoading
    ) {
        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Adaptive(128.dp),
            contentPadding = contentPadding
        ) {
            items(animeList.size, key = { it }) { index ->
                AnimeItem(anime = animeList[index], onAnimeClicked = onAnimeCLicked)
            }
        }
    }
}