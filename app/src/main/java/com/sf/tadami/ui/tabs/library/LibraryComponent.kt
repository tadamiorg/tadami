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
    initLoaded : Boolean,
    onAnimeClicked: (anime: LibraryItem) -> Unit,
    onAnimeLongCLicked: (anime: LibraryItem) -> Unit,
    onRefresh: () -> Unit,
    indicatorPadding: PaddingValues = PaddingValues(0.dp),
    onEmptyRefreshClicked : () -> Unit,
    isRefreshing: Boolean
) {
    LibraryAnimeGrid(
        modifier = modifier,
        animeList = libraryList,
        initLoaded = initLoaded,
        librarySize = librarySize,
        onAnimeClicked = onAnimeClicked,
        onEmptyRefreshClicked = onEmptyRefreshClicked,
        onAnimeLongClicked = onAnimeLongCLicked,
    )
}