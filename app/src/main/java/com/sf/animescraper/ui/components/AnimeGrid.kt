package com.sf.animescraper.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.sf.animescraper.domain.anime.Anime
import com.sf.animescraper.domain.anime.FavoriteAnime
import com.sf.animescraper.domain.anime.toAnime
import com.sf.animescraper.ui.base.widgets.ContentLoader

@Composable
fun AnimeGrid(
    modifier: Modifier = Modifier,
    animeList: LazyPagingItems<Anime>,
    onAnimeCLicked: (anime: Anime) -> Unit,
    lazyGridState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {

    var initialLoading by rememberSaveable {
        mutableStateOf(true)
    }

    initialLoading = when (animeList.loadState.refresh) {
        is LoadState.Loading -> {
            initialLoading
        }
        else -> {
            false
        }
    }

    ContentLoader(
        modifier = modifier,
        isLoading = initialLoading
    ) {
        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Adaptive(128.dp),
            contentPadding = contentPadding
        ) {
            if (animeList.loadState.prepend is LoadState.Loading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LoadingItem()
                }
            }
            items(animeList.itemCount) { index ->
                AnimeGridItem(anime = animeList[index]!!, onAnimeClicked = onAnimeCLicked)
            }
            if (animeList.loadState.refresh is LoadState.Loading || animeList.loadState.append is LoadState.Loading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LoadingItem()
                }
            }
        }
    }
}

@Composable
fun AnimeGrid(
    modifier: Modifier = Modifier,
    animeList: List<FavoriteAnime>,
    onAnimeCLicked: (anime: Anime) -> Unit,
    lazyGridState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyVerticalGrid(
        modifier = modifier,
        state = lazyGridState,
        columns = GridCells.Adaptive(128.dp),
        contentPadding = contentPadding
    ) {
        items(animeList){favorite ->
            CompactAnimeGridItem(anime = favorite.toAnime(), unseenBadge = favorite.unseenEpisodes,onAnimeClicked = onAnimeCLicked)
        }
    }
}

@Composable
private fun LoadingItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}
