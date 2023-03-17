package com.sf.animescraper.ui.tabs.favorites

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sf.animescraper.domain.anime.Anime
import com.sf.animescraper.ui.components.FavoriteAnimeGrid
import com.sf.animescraper.ui.components.data.FavoriteItem

@Composable
fun FavoritesComponent(
    modifier: Modifier = Modifier,
    favoriteList: List<FavoriteItem>,
    onAnimeCLicked: (anime: FavoriteItem) -> Unit,
    onAnimeLongCLicked: (anime: FavoriteItem) -> Unit,

) {
    FavoriteAnimeGrid(
        modifier = modifier,
        animeList = favoriteList,
        onAnimeCLicked = onAnimeCLicked,
        onAnimeLongClicked = onAnimeLongCLicked
    )
}