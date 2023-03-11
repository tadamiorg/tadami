package com.sf.animescraper.ui.discover.search

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import com.sf.animescraper.domain.anime.Anime
import com.sf.animescraper.ui.components.InfiniteAnimeGrid

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SearchComponent(
    modifier: Modifier = Modifier,
    animeList: LazyPagingItems<Anime>,
    onAnimeClicked: (anime: Anime) -> Unit,
    fabPadding: PaddingValues
) {

    Box(modifier = modifier) {
        InfiniteAnimeGrid(
            contentPadding = fabPadding,
            animeList = animeList,
            onAnimeCLicked = onAnimeClicked,
        )
    }
}