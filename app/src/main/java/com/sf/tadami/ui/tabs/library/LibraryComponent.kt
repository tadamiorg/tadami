package com.sf.tadami.ui.tabs.library

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sf.tadami.ui.components.widgets.PullRefresh
import com.sf.tadami.ui.components.grid.LibraryAnimeGrid
import com.sf.tadami.ui.components.data.LibraryItem

@Composable
fun LibraryComponent(
    modifier: Modifier = Modifier,
    libraryList: List<LibraryItem>,
    librarySize : Int,
    onAnimeClicked: (anime: LibraryItem) -> Unit,
    onAnimeLongCLicked: (anime: LibraryItem) -> Unit,
    onRefresh: () -> Unit,
    indicatorPadding: PaddingValues = PaddingValues(0.dp),
    onEmptyRefreshClicked : () -> Unit,
    isRefreshing: Boolean
) {
    PullRefresh(
        refreshing = isRefreshing,
        onRefresh = onRefresh,
        indicatorPadding = indicatorPadding
    ) {
        LibraryAnimeGrid(
            modifier = modifier,
            animeList = libraryList,
            librarySize = librarySize,
            onAnimeClicked = onAnimeClicked,
            onEmptyRefreshClicked = onEmptyRefreshClicked,
            onAnimeLongClicked = onAnimeLongCLicked,
        )
    }
}