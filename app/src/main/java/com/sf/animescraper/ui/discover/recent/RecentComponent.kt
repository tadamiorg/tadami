package com.sf.animescraper.ui.discover.recent

import androidx.compose.runtime.*
import com.sf.animescraper.network.scraping.dto.search.Anime
import com.sf.animescraper.ui.components.InfiniteAnimeGrid

@Composable
fun RecentComponent(
    animeList: List<Anime>,
    onAnimeClicked: (anime: Anime) -> Unit,
    onLoad : () -> Unit,
    isLoading : Boolean
) {
    InfiniteAnimeGrid(animeList = animeList, onAnimeCLicked = onAnimeClicked, onLoad = onLoad,isLoading = isLoading)
}





