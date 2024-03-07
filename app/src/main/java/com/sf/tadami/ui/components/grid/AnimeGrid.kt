package com.sf.tadami.ui.components.grid

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyGridState
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.foundation.lazy.grid.rememberTvLazyGridState
import androidx.tv.material3.Border
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardColors
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardLayoutDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.StandardCardLayout
import com.sf.tadami.R
import com.sf.tadami.data.anime.NoResultException
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.toAnime
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.ui.animeinfos.details.infos.AnimeCover
import com.sf.tadami.ui.components.data.LibraryItem
import com.sf.tadami.ui.components.widgets.ContentLoader
import com.sf.tadami.ui.tabs.library.badges.UnseenBadge
import com.sf.tadami.ui.utils.CommonAnimeItemDefaults
import com.sf.tadami.ui.utils.padding
import com.sf.tadami.ui.utils.plus

@Composable
fun AnimeGrid(
    modifier: Modifier = Modifier,
    animeList: LazyPagingItems<Anime>,
    onAnimeClicked: (anime: Anime) -> Unit,
    onAnimeLongClicked: (anime: Anime) -> Unit = onAnimeClicked,
    lazyGridState: LazyGridState = rememberLazyGridState(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {

    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val libraryPreferences by rememberDataStoreState(LibraryPreferences).value.collectAsState()

    val errorState = animeList.loadState.refresh.takeIf { it is LoadState.Error }
        ?: animeList.loadState.append.takeIf { it is LoadState.Error }

    val getErrorMessage: (LoadState.Error) -> String = { state ->
        when {
            state.error is NoResultException -> context.getString(R.string.pager_no_results)
            state.error.message.orEmpty().startsWith("HTTP error") -> "${state.error.message}: "
            else -> state.error.message.orEmpty()
        }
    }

    LaunchedEffect(errorState) {
        if (animeList.itemCount > 0 && errorState != null && errorState is LoadState.Error) {
            val result = snackbarHostState.showSnackbar(
                message = getErrorMessage(errorState),
                actionLabel = context.getString(R.string.retry),
                duration = SnackbarDuration.Indefinite,
            )
            when (result) {
                SnackbarResult.Dismissed -> snackbarHostState.currentSnackbarData?.dismiss()
                SnackbarResult.ActionPerformed -> animeList.retry()
            }
        }
    }

    var isLoading by rememberSaveable {
        mutableStateOf(true)
    }

    var isNavigated by rememberSaveable {
        mutableStateOf(false)
    }

    var rotationChanged by rememberSaveable {
        mutableStateOf(configuration.orientation)
    }

    LaunchedEffect(animeList.itemCount, animeList.loadState.refresh) {
        if (!isNavigated && configuration.orientation == rotationChanged) {
            isLoading = animeList.itemCount == 0 && animeList.loadState.refresh is LoadState.Loading
        } else {
            isNavigated = false
            rotationChanged = configuration.orientation
        }
    }

    val columns = {
        val number = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            libraryPreferences.landscapeColumns
        } else {
            libraryPreferences.portraitColumns
        }
        if (number == 0) GridCells.Adaptive(128.dp) else GridCells.Fixed(number)
    }

    ContentLoader(
        modifier = modifier,
        isLoading = isLoading
    ) {
        if (animeList.itemCount <= 0 && errorState != null && errorState is LoadState.Error) {
            EmptyScreen(
                message = getErrorMessage(errorState),
                actions = listOf(
                    EmptyScreenAction(
                        stringResId = R.string.refresh,
                        icon = Icons.Outlined.Refresh,
                        onClick = animeList::retry
                    )
                )
            )
            return@ContentLoader
        }

        LazyVerticalGrid(
            state = lazyGridState,
            columns = columns(),
            verticalArrangement = Arrangement.spacedBy(CommonAnimeItemDefaults.GridVerticalSpacer),
            horizontalArrangement = Arrangement.spacedBy(CommonAnimeItemDefaults.GridHorizontalSpacer),
            contentPadding = contentPadding + PaddingValues(MaterialTheme.padding.extraSmall)
        ) {
            if (animeList.loadState.prepend is LoadState.Loading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LoadingItem()
                }
            }
            items(animeList.itemCount) { index ->
                CompactAnimeGridItem(
                    anime = animeList[index]!!,
                    onClick = {
                        onAnimeClicked(animeList[index]!!)
                        isNavigated = true
                    },
                    onLongClick = { onAnimeLongClicked(animeList[index]!!) }
                )
            }
            if (animeList.loadState.append is LoadState.Loading) {
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
    librarySize: Int,
    initLoaded: Boolean,
    onAnimeClicked: (anime: LibraryItem) -> Unit,
    onAnimeLongClicked: (anime: LibraryItem) -> Unit,
    lazyGridState: TvLazyGridState = rememberTvLazyGridState(),
    onEmptyRefreshClicked: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {

    val libraryPreferences by rememberDataStoreState(LibraryPreferences).value.collectAsState()

    val configuration = LocalConfiguration.current

    val columns = {
        val number = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            libraryPreferences.landscapeColumns
        } else {
            libraryPreferences.portraitColumns
        }
        if (number == 0) TvGridCells.Adaptive(128.dp) else TvGridCells.Fixed(number)
    }

    LaunchedEffect(key1 = animeList.firstOrNull()) {
        if (animeList.firstOrNull() != null) {
            lazyGridState.animateScrollToItem(0)
        }
    }

    TvLazyVerticalGrid(
        modifier = modifier,
        state = lazyGridState,
        columns = columns(),
        verticalArrangement = Arrangement.spacedBy(CommonAnimeItemDefaults.GridVerticalSpacer),
        horizontalArrangement = Arrangement.spacedBy(CommonAnimeItemDefaults.GridHorizontalSpacer),
        contentPadding = contentPadding + PaddingValues(MaterialTheme.padding.large)
    ) {
        items(animeList, key = { it.anime.id }) { libraryItem ->
            StandardCardLayout(
                contentColor = CardLayoutDefaults.contentColor(
                    contentColor = Color.Transparent,
                    focusedContentColor = Color.Transparent,
                    pressedContentColor = Color.Transparent
                ),
                modifier = Modifier,
                imageCard = {
                    CardLayoutDefaults.ImageCard(
                        colors = CardDefaults.colors(
                            containerColor = Color.Transparent
                        ),
                        onClick = {
                            onAnimeClicked(libraryItem)
                        },
                        onLongClick = {
                            onAnimeLongClicked(libraryItem)
                        },
                        interactionSource = it,
                        border = CardDefaults.border(
                            border = Border.None,
                            focusedBorder = Border.None,
                            pressedBorder = Border.None
                        )
                    ) {
                        CompactAnimeGridItem(
                            isSelected = libraryItem.selected,
                            anime = libraryItem.anime.toAnime(),
                            onClick = {
                            },
                            onLongClick = {
                            },
                            coverBadgeStart = {
                                UnseenBadge(count = libraryItem.anime.unseenEpisodes)
                            }
                        )
                    }

                },
                title = {}
            )

        }
    }
}

@Composable
private fun LoadingItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.padding.medium),
        horizontalArrangement = Arrangement.Center,
    ) {

    }
}
