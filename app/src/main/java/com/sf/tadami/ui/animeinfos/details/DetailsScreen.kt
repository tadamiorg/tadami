package com.sf.tadami.ui.animeinfos.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.AnimeInfosRoutes
import com.sf.tadami.ui.animeinfos.details.episodes.EpisodesHeader
import com.sf.tadami.ui.animeinfos.details.episodes.episodeItems
import com.sf.tadami.ui.animeinfos.details.infos.AnimeInfosBox
import com.sf.tadami.ui.animeinfos.details.infos.description.ExpandableAnimeDescription
import com.sf.tadami.ui.components.bottombar.ContextualBottomBar
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.widgets.PullRefresh
import com.sf.tadami.ui.components.widgets.VerticalFastScroller
import com.sf.tadami.ui.utils.isScrollingUp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    navHostController: NavHostController,
    detailsViewModel: DetailsViewModel = viewModel()
) {

    val uiState by detailsViewModel.uiState.collectAsState()
    val isRefreshing by detailsViewModel.isRefreshing.collectAsState()

    val episodesListState = rememberLazyListState()

    var fabHeight by remember {
        mutableStateOf(0)
    }

    val fabHeightInDp = with(LocalDensity.current) { fabHeight.toDp() }

    Scaffold(
        topBar = {
            DetailsToolbar(
                title = uiState.details?.title ?: "",
                episodesListState = episodesListState,
                onBackClicked = { navHostController.navigateUp() },
                onLibraryAnimeClicked = {
                    detailsViewModel.toggleFavorite()
                },
                actionModeCounter = uiState.episodes.count { it.selected },
                onCloseClicked = {
                    detailsViewModel.toggleAllSelectedEpisodes(false)
                },
                onInverseAll = {
                    detailsViewModel.inverseSelectedEpisodes()
                },
                onToggleAll = {
                    detailsViewModel.toggleAllSelectedEpisodes(true)
                },
                isFavorited = uiState.details?.favorite
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = uiState.episodes.fastAll { !it.selected } && uiState.episodes.fastAny { !it.episode.seen },
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                DisposableEffect(
                    ExtendedFloatingActionButton(
                        modifier = Modifier.onGloballyPositioned {
                            fabHeight = it.size.height
                        },
                        text = {
                            Text(
                                text = if (uiState.episodes.fastAny { it.episode.seen })
                                    stringResource(id = R.string.details_screen_resume_button)
                                else stringResource(id = R.string.details_screen_start_button)
                            )
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_play),
                                contentDescription = null
                            )
                        },
                        onClick = {
                            val resumedEpisode = uiState.episodes.reversed().find { !it.episode.seen }
                            resumedEpisode?.let{
                                navHostController.navigate("${AnimeInfosRoutes.EPISODE}/${detailsViewModel.source.id}/${it.episode.id}")
                            }
                        },
                        expanded = episodesListState.isScrollingUp()

                    )
                ) {
                    onDispose {
                        fabHeight = 0
                    }
                }

            }

        },
        bottomBar = {
            ContextualBottomBar(
                visible = uiState.episodes.fastAny { it.selected },
                actions = listOf(
                    Action.Vector(
                        title = R.string.stub_text,
                        icon = Icons.Outlined.DoneAll,
                        onClick = {
                            detailsViewModel.setSeenStatus()
                        },
                    ),
                    Action.Vector(
                        title = R.string.stub_text,
                        icon = Icons.Outlined.RemoveDone,
                        onClick = {
                            detailsViewModel.setUnseenStatus()
                        },
                    ),
                    Action.Drawable(
                        title = R.string.stub_text,
                        icon = R.drawable.done_down,
                        onClick = {
                            detailsViewModel.setSeenStatusDown()
                        },
                        enabled = uiState.episodes.count { it.selected } == 1
                    )
                )
            )
        }
    ) { contentPadding ->
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
                        bottom = contentPadding.calculateBottomPadding() + fabHeightInDp + 15.dp,
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
                        onEpisodeSelected = { episodeItem, selected ->
                            detailsViewModel.toggleSelectedEpisode(episodeItem, selected)
                        }
                    )
                }
            }
        }
    }
}