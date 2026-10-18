package com.sf.tadami.ui.tabs.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sf.tadami.ui.components.data.LibraryItem
import com.sf.tadami.ui.components.grid.LibraryAnimeGrid
import com.sf.tadami.ui.components.widgets.PullRefresh

@Composable
fun LibraryComponent(
    modifier: Modifier = Modifier,
    libraryList: List<LibraryItem>,
    librarySize : Int,
    // The category of the tab has no anime, not counting the search and the filters
    categoryEmpty : Boolean = false,
    initLoaded : Boolean,
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
            initLoaded = initLoaded,
            librarySize = librarySize,
            categoryEmpty = categoryEmpty,
            onAnimeClicked = onAnimeClicked,
            onEmptyRefreshClicked = onEmptyRefreshClicked,
            onAnimeLongClicked = onAnimeLongCLicked,
        )
    }
}