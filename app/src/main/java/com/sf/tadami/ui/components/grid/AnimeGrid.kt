package com.sf.tadami.ui.components.grid

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.toAnime
import com.sf.tadami.ui.components.widgets.ContentLoader
import com.sf.tadami.ui.components.data.LibraryItem
import com.sf.tadami.ui.tabs.settings.model.rememberDataStoreState
import com.sf.tadami.ui.tabs.settings.screens.library.LibraryPreferences
import com.sf.tadami.ui.utils.CommonMangaItemDefaults
import com.sf.tadami.ui.utils.plus

@Composable
fun AnimeGrid(
    modifier: Modifier = Modifier,
    animeList: LazyPagingItems<Anime>,
    onAnimeClicked: (anime: Anime) -> Unit,
    onAnimeLongClicked : (anime: Anime) -> Unit = onAnimeClicked,
    lazyGridState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {

    val libraryPreferences by rememberDataStoreState(LibraryPreferences).value.collectAsState()

    var initialLoading by rememberSaveable {
        mutableStateOf(true)
    }

    initialLoading = when (animeList.loadState.refresh) {
        is LoadState.Loading -> {
            initialLoading
        }
        else -> {
            false
        }
    }

    val configuration = LocalConfiguration.current

    val columns = {
        val number = if(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
            libraryPreferences.landscapeColumns
        }else{
            libraryPreferences.portraitColumns
        }
        if(number==0) GridCells.Adaptive(128.dp) else GridCells.Fixed(number)
    }


    ContentLoader(
        modifier = modifier,
        isLoading = initialLoading
    ) {
        LazyVerticalGrid(
            state = lazyGridState,
            columns = columns(),
            verticalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridVerticalSpacer),
            horizontalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridHorizontalSpacer),
            contentPadding = contentPadding + PaddingValues(8.dp)
        ) {
            if (animeList.loadState.prepend is LoadState.Loading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LoadingItem()
                }
            }
            items(animeList.itemCount) { index ->
                CompactAnimeGridItem(
                    anime = animeList[index]!!,
                    onClick = { onAnimeClicked(animeList[index]!!) },
                    onLongClick = { onAnimeLongClicked(animeList[index]!!) }
                )
            }
            if (animeList.loadState.refresh is LoadState.Loading || animeList.loadState.append is LoadState.Loading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LoadingItem()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryAnimeGrid(
    modifier: Modifier = Modifier,
    animeList: List<LibraryItem>,
    onAnimeCLicked: (anime: LibraryItem) -> Unit,
    onAnimeLongClicked: (anime: LibraryItem) -> Unit,
    lazyGridState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {

    val libraryPreferences by rememberDataStoreState(LibraryPreferences).value.collectAsState()

    val configuration = LocalConfiguration.current

    val columns = {
        val number = if(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
            libraryPreferences.landscapeColumns
        }else{
            libraryPreferences.portraitColumns
        }
        if(number==0) GridCells.Adaptive(128.dp) else GridCells.Fixed(number)
    }

    LaunchedEffect(key1 = animeList.firstOrNull()) {
        if(animeList.firstOrNull() != null){
            lazyGridState.animateScrollToItem(0)
        }
    }

    LazyVerticalGrid(
        modifier = modifier,
        state = lazyGridState,
        columns = columns(),
        verticalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridVerticalSpacer),
        horizontalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridHorizontalSpacer),
        contentPadding = contentPadding + PaddingValues(8.dp)
    ) {
        items(animeList,key= {it.anime.id}) { libraryItem ->
            CompactAnimeGridItem(
                modifier = Modifier.animateItemPlacement(),
                isSelected = libraryItem.selected,
                anime = libraryItem.anime.toAnime(),
                unseenBadge = libraryItem.anime.unseenEpisodes,
                onClick = {
                    onAnimeCLicked(libraryItem)
                },
                onLongClick = {
                    onAnimeLongClicked(libraryItem)
                }
            )
        }
    }
}

@Composable
private fun LoadingItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}
