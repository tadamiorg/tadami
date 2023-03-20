package com.sf.animescraper.ui.components

import android.content.res.Configuration
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
import com.sf.animescraper.domain.anime.Anime
import com.sf.animescraper.domain.anime.toAnime
import com.sf.animescraper.ui.base.widgets.ContentLoader
import com.sf.animescraper.ui.components.data.FavoriteItem
import com.sf.animescraper.ui.tabs.settings.model.rememberDataStoreState
import com.sf.animescraper.ui.tabs.settings.screens.library.LibraryPreferences
import com.sf.animescraper.ui.utils.CommonMangaItemDefaults
import com.sf.animescraper.ui.utils.plus

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

@Composable
fun FavoriteAnimeGrid(
    modifier: Modifier = Modifier,
    animeList: List<FavoriteItem>,
    onAnimeCLicked: (anime: FavoriteItem) -> Unit,
    onAnimeLongClicked: (anime: FavoriteItem) -> Unit,
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

    LazyVerticalGrid(
        modifier = modifier,
        state = lazyGridState,
        columns = columns(),
        verticalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridVerticalSpacer),
        horizontalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridHorizontalSpacer),
        contentPadding = contentPadding + PaddingValues(8.dp)
    ) {
        items(animeList) { favoriteItem ->
            CompactAnimeGridItem(
                isSelected = favoriteItem.selected,
                anime = favoriteItem.anime.toAnime(),
                unseenBadge = favoriteItem.anime.unseenEpisodes,
                onClick = {
                    onAnimeCLicked(favoriteItem)
                },
                onLongClick = {
                    onAnimeLongClicked(favoriteItem)
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
