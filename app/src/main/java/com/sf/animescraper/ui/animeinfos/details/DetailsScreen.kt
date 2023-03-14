package com.sf.animescraper.ui.animeinfos.details

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    navHostController: NavHostController,
    detailsViewModel: DetailsViewModel = viewModel()
) {

    val uiState by detailsViewModel.uiState.collectAsState()
    val isRefreshing by detailsViewModel.isRefreshing.collectAsState()

    val episodesListState = rememberLazyListState()

    Scaffold(topBar = {
        StateDetailsToolbar(
            title = uiState.details?.title ?: "",
            episodesListState = episodesListState,
            onBackClicked = { navHostController.navigateUp() },
            onFavoriteClicked = {
                detailsViewModel.toggleFavorite()
            }
        )
    }) { contentPadding ->
        val topPadding = contentPadding.calculateTopPadding()

        PullRefresh(
            refreshing = isRefreshing,
            onRefresh = {
                detailsViewModel.onRefresh()
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
                            title = uiState.details?.title ?: "",
                            author = uiState.details?.release,
                            artist = "",
                            status = uiState.details?.status,
                            cover = { uiState.details?.thumbnailUrl ?: "" },
                            sourceName = detailsViewModel.source.name
                        )
                    }

                    item(
                        key = DetailsScreenItem.DESCRIPTION_WITH_TAG,
                        contentType = DetailsScreenItem.DESCRIPTION_WITH_TAG,
                    ) {
                        ExpandableAnimeDescription(
                            defaultExpandState = false,
                            description = uiState.details?.description,
                            tagsProvider = { uiState.details?.genres },
                        )
                    }

                    item(
                        key = DetailsScreenItem.EPISODE_HEADER,
                        contentType = DetailsScreenItem.EPISODE_HEADER,
                    ) {
                        EpisodesHeader(
                            modifier = Modifier.padding(16.dp),
                            episodesNumber = uiState.episodes.size
                        )
                    }

                    episodeItems(
                        episodes = uiState.episodes,
                        onEpisodeClicked = { epId ->
                            navHostController.navigate("${AnimeInfosRoutes.EPISODE}/${detailsViewModel.source.id}/$epId")
                        },
                        onEpisodeLongClick = {

                        }
                    )
                }
            }
        }
    }
}