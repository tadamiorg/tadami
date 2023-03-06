package com.sf.animescraper.ui.discover.search

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sf.animescraper.network.scraping.dto.search.Anime
import com.sf.animescraper.ui.components.InfiniteAnimeGrid

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SearchComponent(
    modifier: Modifier = Modifier,
    animeList: List<Anime>,
    onAnimeClicked: (anime: Anime) -> Unit,
    onLoad: () -> Unit,
    fabPadding: PaddingValues,
    isLoading: Boolean
) {

    Box(modifier = modifier) {
        InfiniteAnimeGrid(
            contentPadding = fabPadding,
            animeList = animeList,
            onAnimeCLicked = onAnimeClicked,
            onLoad = onLoad,
            isLoading = isLoading
        )
    }
}