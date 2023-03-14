package com.sf.animescraper.ui.tabs.favorites

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sf.animescraper.domain.anime.Anime
import com.sf.animescraper.domain.anime.FavoriteAnime
import com.sf.animescraper.ui.components.AnimeGrid

@Composable
fun FavoritesComponent(
    modifier: Modifier = Modifier,
    favoriteList: List<FavoriteAnime>,
    onFavoriteClicked: (anime: Anime) -> Unit
) {
    AnimeGrid(
        modifier = modifier,
        animeList = favoriteList,
        onAnimeCLicked = onFavoriteClicked
    )
}