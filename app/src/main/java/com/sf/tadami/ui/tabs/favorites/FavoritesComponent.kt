package com.sf.tadami.ui.tabs.favorites

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sf.tadami.ui.base.widgets.PullRefresh
import com.sf.tadami.ui.components.FavoriteAnimeGrid
import com.sf.tadami.ui.components.data.FavoriteItem

@Composable
fun FavoritesComponent(
    modifier: Modifier = Modifier,
    favoriteList: List<FavoriteItem>,
    onAnimeCLicked: (anime: FavoriteItem) -> Unit,
    onAnimeLongCLicked: (anime: FavoriteItem) -> Unit,
    onRefresh : () -> Unit,
    indicatorPadding : PaddingValues = PaddingValues(0.dp),
    isRefreshing : Boolean

) {
    PullRefresh(
        refreshing = isRefreshing,
        onRefresh = onRefresh,
        indicatorPadding = indicatorPadding
    ) {
        FavoriteAnimeGrid(
            modifier = modifier,
            animeList = favoriteList,
            onAnimeCLicked = onAnimeCLicked,
            onAnimeLongClicked = onAnimeLongCLicked
        )
    }
}