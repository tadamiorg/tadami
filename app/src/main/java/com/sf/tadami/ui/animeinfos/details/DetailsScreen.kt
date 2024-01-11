package com.sf.tadami.ui.animeinfos.details

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.animeInfos.AnimeInfosRoutes
import com.sf.tadami.network.api.online.StubSource
import com.sf.tadami.ui.animeinfos.details.episodes.filters.DisplayTab
import com.sf.tadami.ui.animeinfos.details.episodes.filters.FilterTab
import com.sf.tadami.ui.components.bottombar.ContextualBottomBar
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.dialog.sheets.TabContent
import com.sf.tadami.ui.components.dialog.sheets.TabbedBottomSheet
import com.sf.tadami.ui.components.filters.TadaBottomSheetLayout
import com.sf.tadami.ui.components.material.ExtendedFloatingActionButton
import com.sf.tadami.ui.components.material.Scaffold
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.utils.isScrolledToEnd
import com.sf.tadami.ui.utils.isScrollingUp
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("OpaqueUnitKey")
@Composable
fun DetailsScreen(
    navHostController: NavHostController,
    detailsViewModel: DetailsViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val uiState by detailsViewModel.uiState.collectAsState()
    val isRefreshing by detailsViewModel.isRefreshing.collectAsState()

    val episodesListState = rememberLazyListState()

    val filtersSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = false
    )

    TadaBottomSheetLayout(
        sheetContent = {
            if (uiState.details != null) {
                TabbedBottomSheet(
                    sheetState = filtersSheetState,
                    tabs = listOf(
                        TabContent(
                            titleRes = R.string.action_filter
                        ) {
                            FilterTab(
                                anime = uiState.details!!,
                                setFilters = {
                                    detailsViewModel.setEpisodeFlags(it)
                                }
                            )
                        },
                        TabContent(
                            titleRes = R.string.title_display
                        ) {
                            DisplayTab(
                                anime = uiState.details!!,
                                setDisplayMode = {
                                    detailsViewModel.setEpisodeFlags(it)
                                }
                            )
                        }
                    )
                )
            }
        },
        sheetState = filtersSheetState
    ) {
        Scaffold(
            topBar = {
                DetailsToolbar(
                    title = uiState.details?.title ?: "",
                    episodesListState = episodesListState,
                    onBackClicked = { navHostController.navigateUp() },
                    actionModeCounter = uiState.episodes.count { it.selected },
                    onCloseClicked = {
                        detailsViewModel.toggleAllSelectedEpisodes(false)
                    },
                    onInverseAll = {
                        detailsViewModel.inverseSelectedEpisodes()
                    },
                    onToggleAll = {
                        detailsViewModel.toggleAllSelectedEpisodes(true)
                    }
                )
            },
            floatingActionButton = {
                val isFABVisible = remember(uiState.episodes) {
                    uiState.episodes.fastAll { !it.selected } && uiState.episodes.fastAny { !it.episode.seen }
                }
                AnimatedVisibility(
                    visible = isFABVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    ExtendedFloatingActionButton(
                        text = {
                            val isWatching = remember(uiState.episodes) {
                                uiState.episodes.fastAny { it.episode.seen }
                            }
                            Text(
                                text = stringResource(if (isWatching) R.string.details_screen_resume_button else R.string.details_screen_start_button)
                            )
                        },
                        icon = {
                            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                        },
                        onClick = {
                            val resumedEpisode =
                                uiState.episodes.reversed().find { !it.episode.seen }
                            resumedEpisode?.let {
                                navHostController.navigate("${AnimeInfosRoutes.EPISODE}/${detailsViewModel.source.id}/${it.episode.id}")
                            }
                        },
                        expanded = episodesListState.isScrollingUp() || episodesListState.isScrolledToEnd()

                    )
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
            DetailsComponent(
                isRefreshing = isRefreshing,
                contentPadding = contentPadding,
                episodesListState = episodesListState,
                uiState = uiState,
                sourceName = detailsViewModel.source.name,
                isStubSource = remember { detailsViewModel.source is StubSource },
                onRefresh = detailsViewModel::onRefresh,
                onAddToLibraryClicked = detailsViewModel::toggleFavorite,
                onWebViewClicked = {
                    val url = uiState.details?.url
                    val title = uiState.details?.title
                    if (url != null && title != null) {
                        if (detailsViewModel.source !is StubSource) {
                            val encodedUrl = URLEncoder.encode(
                                uiState.details?.url,
                                StandardCharsets.UTF_8.toString()
                            )
                            navHostController.navigate("${AnimeInfosRoutes.WEBVIEW}/${detailsViewModel.source.id}/$title/$encodedUrl")
                        } else {
                            UiToasts.showToast(
                                stringRes = R.string.source_not_installed,
                                args = arrayOf(detailsViewModel.source.id)
                            )
                        }
                    }
                },
                onEpisodeClicked = { epId ->
                    navHostController.navigate("${AnimeInfosRoutes.EPISODE}/${detailsViewModel.source.id}/$epId")
                },
                onEpisodeSelected = { episodeItem, selected ->
                    detailsViewModel.toggleSelectedEpisode(episodeItem, selected)
                },
                onEpisodeFilterClicked = {
                    coroutineScope.launch {
                        filtersSheetState.show()
                    }
                }
            )
        }
    }


}