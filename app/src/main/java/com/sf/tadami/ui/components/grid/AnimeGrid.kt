package com.sf.tadami.ui.components.grid

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.sf.tadami.R
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.toAnime
import com.sf.tadami.network.CloudflareChallengeException
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.ui.components.data.LibraryItem
import com.sf.tadami.ui.components.widgets.CloudflareVerificationLine
import com.sf.tadami.ui.components.widgets.ContentLoader
import com.sf.tadami.ui.tabs.library.badges.UnseenBadge
import com.sf.tadami.ui.utils.CommonAnimeItemDefaults
import com.sf.tadami.ui.utils.formattedMessage
import com.sf.tadami.ui.utils.padding
import com.sf.tadami.ui.utils.plus
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun AnimeGrid(
    modifier: Modifier = Modifier,
    animeList: LazyPagingItems<Anime>,
    onAnimeClicked: (anime: Anime) -> Unit,
    onAnimeLongClicked: (anime: Anime) -> Unit = onAnimeClicked,
    lazyGridState: LazyGridState = rememberLazyGridState(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onOpenInWebView: ((url: String) -> Unit)? = null,
    sourceBaseUrl: String? = null,
) {

    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val libraryPreferences by rememberDataStoreState(LibraryPreferences).value.collectAsState()

    val errorState = animeList.loadState.refresh.takeIf { it is LoadState.Error }
        ?: animeList.loadState.append.takeIf { it is LoadState.Error }

    val getErrorMessage: (LoadState.Error) -> String = { state ->
        with(context) { state.error.formattedMessage }
    }

    // Solving the challenge in the WebView is the way out, refreshing would just fail again
    val challengeUrl = ((errorState as? LoadState.Error)?.error as? CloudflareChallengeException)?.url
        ?.takeIf { onOpenInWebView != null }

    // The WebView is another activity (opened from here or from the top bar): once back with a
    // clearance, retry the failed load with it. Without one a retry would only run into the
    // challenge again, so the error and its WebView action stay
    val currentChallengeUrl by rememberUpdatedState(challengeUrl)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val url = currentChallengeUrl ?: return@LifecycleEventEffect
        if (Injekt.get<NetworkHelper>().cookieManager.hasCloudflareClearance(url)) {
            animeList.retry()
        }
    }

    LaunchedEffect(errorState) {
        if (animeList.itemCount > 0 && errorState != null && errorState is LoadState.Error) {
            val result = snackbarHostState.showSnackbar(
                message = getErrorMessage(errorState),
                actionLabel = context.getString(
                    if (challengeUrl != null) R.string.action_open_in_webview else R.string.retry,
                ),
                duration = SnackbarDuration.Indefinite,
            )
            when (result) {
                SnackbarResult.Dismissed -> snackbarHostState.currentSnackbarData?.dismiss()
                SnackbarResult.ActionPerformed -> {
                    if (challengeUrl != null) onOpenInWebView?.invoke(challengeUrl) else animeList.retry()
                }
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

    LaunchedEffect(animeList.itemCount,animeList.loadState.refresh){
        if(!isNavigated && configuration.orientation == rotationChanged){
            isLoading = animeList.itemCount == 0 && animeList.loadState.refresh is LoadState.Loading
        }
        else{
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
        isLoading = isLoading,
        subtitle = { CloudflareVerificationLine(sourceBaseUrl) },
    ) {
        if (animeList.itemCount <= 0 && errorState != null && errorState is LoadState.Error) {
            EmptyScreen(
                // Centered in the visible area, not under the top bar
                modifier = Modifier.padding(contentPadding),
                message = getErrorMessage(errorState),
                actions = listOfNotNull(
                    challengeUrl?.let { url ->
                        EmptyScreenAction(
                            stringResId = R.string.action_open_in_webview,
                            icon = Icons.Outlined.Public,
                            onClick = { onOpenInWebView?.invoke(url) }
                        )
                    },
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
                    LoadingItem(sourceBaseUrl)
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
                    LoadingItem(sourceBaseUrl)
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
    initLoaded : Boolean,
    onAnimeClicked: (anime: LibraryItem) -> Unit,
    onAnimeLongClicked: (anime: LibraryItem) -> Unit,
    lazyGridState: LazyGridState = rememberLazyGridState(),
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
        if (number == 0) GridCells.Adaptive(128.dp) else GridCells.Fixed(number)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            modifier = modifier,
            state = lazyGridState,
            columns = columns(),
            verticalArrangement = Arrangement.spacedBy(CommonAnimeItemDefaults.GridVerticalSpacer),
            horizontalArrangement = Arrangement.spacedBy(CommonAnimeItemDefaults.GridHorizontalSpacer),
            contentPadding = contentPadding + PaddingValues(MaterialTheme.padding.extraSmall)
        ) {
            items(animeList, key = { it.anime.id }) { libraryItem ->
                CompactAnimeGridItem(
                    modifier = Modifier.animateItem(),
                    isSelected = libraryItem.selected,
                    anime = libraryItem.anime.toAnime(),
                    onClick = {
                        onAnimeClicked(libraryItem)
                    },
                    onLongClick = {
                        onAnimeLongClicked(libraryItem)
                    },
                    coverBadgeStart = {
                        UnseenBadge(count = libraryItem.anime.unseenEpisodes)
                    }
                )
            }
        }
        if(!initLoaded){
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center),
                strokeWidth = 2.dp,
            )
        }
        if (librarySize <= 0 && initLoaded) {
            EmptyScreen(
                message = stringResource(id = R.string.empty_library_title), actions = listOf(
                    EmptyScreenAction(
                        stringResId = R.string.empty_library_action,
                        icon = Icons.AutoMirrored.Outlined.ArrowForward,
                        onClick = onEmptyRefreshClicked
                    )
                )
            )
        }
    }
}

@Composable
private fun LoadingItem(sourceBaseUrl: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.padding.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        CloudflareVerificationLine(sourceBaseUrl)
    }
}
