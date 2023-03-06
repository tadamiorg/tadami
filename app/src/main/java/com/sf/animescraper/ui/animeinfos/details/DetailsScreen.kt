package com.sf.animescraper.ui.animeinfos.details

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.animescraper.navigation.graphs.AnimeInfosRoutes
import com.sf.animescraper.ui.animeinfos.details.episodes.EpisodesHeader
import com.sf.animescraper.ui.animeinfos.details.episodes.episodeItems
import com.sf.animescraper.ui.animeinfos.details.infos.AnimeInfosBox
import com.sf.animescraper.ui.animeinfos.details.infos.description.ExpandableAnimeDescription
import com.sf.animescraper.ui.base.widgets.PullRefresh
import com.sf.animescraper.ui.base.widgets.VerticalFastScroller
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    navHostController: NavHostController,
    detailsViewModel: DetailsViewModel = viewModel()
) {

    val detailsUiState by detailsViewModel.uiState.collectAsState()
    val isRefreshing by detailsViewModel.isRefreshing.collectAsState()

    val episodesListState = rememberLazyListState()
    val episodes = remember(detailsUiState) { detailsUiState.episodes }

    Scaffold(topBar = {
        StateDetailsToolbar(
            title = detailsUiState.details?.title ?: "",
            episodesListState = episodesListState,
            onBackClicked = { navHostController.navigateUp() },
            onFavoriteClicked = {


            }
        )
    }) { contentPadding ->
        val topPadding = contentPadding.calculateTopPadding()

        PullRefresh(
            refreshing = isRefreshing,
            onRefresh = {
                detailsViewModel.refresh()
            },
            indicatorPadding = contentPadding,
        ) {
            val layoutDirection = LocalLayoutDirection.current
            VerticalFastScroller(
                listState = episodesListState,
                topContentPadding = topPadding,
                endContentPadding = contentPadding.calculateEndPadding(layoutDirection),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    state = episodesListState,
                    contentPadding = PaddingValues(
                        start = contentPadding.calculateStartPadding(layoutDirection),
                        end = contentPadding.calculateEndPadding(layoutDirection),
                        bottom = contentPadding.calculateBottomPadding(),
                    ),
                ) {
                    item(
                        key = DetailsScreenItem.INFO_BOX,
                        contentType = DetailsScreenItem.INFO_BOX,
                    ) {
                        AnimeInfosBox(
                            appBarPadding = topPadding,
                            title = detailsUiState.details?.title ?: "",
                            author = detailsUiState.details?.release,
                            artist = "",
                            status = detailsUiState.details?.status,
                            cover = { detailsUiState.details?.thumbnail_url ?: "" },
                            sourceName = detailsViewModel.source.name
                        )
                    }

                    item(
                        key = DetailsScreenItem.DESCRIPTION_WITH_TAG,
                        contentType = DetailsScreenItem.DESCRIPTION_WITH_TAG,
                    ) {
                        ExpandableAnimeDescription(
                            defaultExpandState = false,
                            description = detailsUiState.details?.description,
                            tagsProvider = { detailsUiState.details?.genre },
                        )
                    }

                    item(
                        key = DetailsScreenItem.EPISODE_HEADER,
                        contentType = DetailsScreenItem.EPISODE_HEADER,
                    ) {
                        EpisodesHeader(
                            modifier = Modifier.padding(16.dp),
                            episodesNumber = detailsUiState.episodes.size
                        )
                    }

                    episodeItems(episodes = episodes, onEpisodeClicked = { index ->
                        val json: Json = Injekt.get()
                        val params = detailsUiState.episodes
                        val episodesArg = Uri.encode(json.encodeToString(params))
                        val title = detailsUiState.details?.title
                        navHostController.navigate("${AnimeInfosRoutes.EPISODE}/$title/$index?episodes=$episodesArg")
                    })
                }
            }
        }
    }
}