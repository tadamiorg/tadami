package com.sf.animescraper.ui.components

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sf.animescraper.network.scraping.dto.search.Anime
import com.sf.animescraper.ui.utils.OnBottomReached
import kotlinx.coroutines.launch

@Composable
fun InfiniteAnimeGrid(
    modifier: Modifier = Modifier,
    animeList: List<Anime>,
    onAnimeCLicked: (anime: Anime) -> Unit,
    onLoad: () -> Unit,
    contentPadding : PaddingValues = PaddingValues(0.dp),
    isLoading: Boolean
) {

    val lazyGridState = rememberLazyGridState()

    if (isLoading) {
        LaunchedEffect(key1 = Unit) {
            this.launch {
                onLoad()
            }
        }
    }

    if (!isLoading) {
        lazyGridState.OnBottomReached {
            onLoad()
        }
    }

    AnimeGrid(
        modifier = modifier,
        contentPadding = contentPadding,
        animeList = animeList,
        onAnimeCLicked = onAnimeCLicked,
        lazyGridState = lazyGridState,
        isLoading = isLoading
    )

}