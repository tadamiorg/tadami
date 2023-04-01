package com.sf.tadami.ui.tabs.library

import androidx.compose.foundation.layout.PaddingValues
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
    onAnimeCLicked: (anime: LibraryItem) -> Unit,
    onAnimeLongCLicked: (anime: LibraryItem) -> Unit,
    onRefresh : () -> Unit,
    indicatorPadding : PaddingValues = PaddingValues(0.dp),
    isRefreshing : Boolean

) {
    PullRefresh(
        refreshing = isRefreshing,
        onRefresh = onRefresh,
        indicatorPadding = indicatorPadding
    ) {
        LibraryAnimeGrid(
            modifier = modifier,
            animeList = libraryList,
            onAnimeCLicked = onAnimeCLicked,
            onAnimeLongClicked = onAnimeLongCLicked,
        )
    }
}