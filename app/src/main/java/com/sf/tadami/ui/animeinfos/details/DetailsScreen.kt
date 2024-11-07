package com.sf.tadami.ui.animeinfos.details

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.animeInfos.AnimeInfosRoutes
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.navigation.graphs.migrate.MigrateRoutes
import com.sf.tadami.source.StubSource
import com.sf.tadami.source.online.AnimeHttpSource
import com.sf.tadami.ui.animeinfos.details.episodes.filters.DisplayTab
import com.sf.tadami.ui.animeinfos.details.episodes.filters.FilterTab
import com.sf.tadami.ui.components.bottombar.ContextualBottomBar
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.dialog.sheets.TabContent
import com.sf.tadami.ui.components.dialog.sheets.TabbedBottomSheet
import com.sf.tadami.ui.components.filters.TadaBottomSheetScaffold
import com.sf.tadami.ui.components.material.ExtendedFloatingActionButton
import com.sf.tadami.ui.discover.migrate.dialog.MigrateDialog
import com.sf.tadami.ui.discover.migrate.dialog.MigrationState
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.utils.isScrolledToEnd
import com.sf.tadami.ui.utils.isScrollingUp
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("OpaqueUnitKey")
@Composable
fun DetailsScreen(
    navHostController: NavHostController,
    detailsViewModel: DetailsViewModel = viewModel()
) {
    val uiState by detailsViewModel.uiState.collectAsState()
    val isRefreshing by detailsViewModel.isRefreshing.collectAsState()
    val migrateHelperState by detailsViewModel.migrateHelperState.collectAsState()

    val episodesListState = rememberLazyListState()

    val filtersSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showFiltersSheet by remember { mutableStateOf(false) }

    var isMigrationOpened by rememberSaveable {
        mutableStateOf(false)
    }

    Box {
        TadaBottomSheetScaffold(
            showSheet = showFiltersSheet && uiState.details != null,
            sheetState = filtersSheetState,
            onDismissSheet = {
                showFiltersSheet = false
            },
            sheetContent = {
                TabbedBottomSheet(
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
            },
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
                    },
                    migrationEnabled = uiState.details?.favorite == true && uiState.details?.id != null && migrateHelperState.oldAnime == null,
                    onMigrateClicked = {
                        navHostController.navigate("${MigrateRoutes.MIGRATE}/${uiState.details!!.id}")
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
                onAddToLibraryClicked = {
                    if (migrateHelperState.oldAnime != null) {
                        isMigrationOpened = true
                    } else {
                        detailsViewModel.toggleFavorite()
                    }
                },
                onWebViewClicked = {
                    val url = uiState.details?.url
                    val title = uiState.details?.title
                    if (url != null && title != null) {
                        if (detailsViewModel.source !is StubSource) {
                            val encodedUrl = URLEncoder.encode(
                                (detailsViewModel.source as AnimeHttpSource).baseUrl + uiState.details?.url,
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
                    showFiltersSheet = true
                }
            )
        }
        MigrateDialog(
            opened = isMigrationOpened,
            oldAnime = migrateHelperState.oldAnime,
            newAnime = migrateHelperState.newAnime,
            enableShowTitle = false,
            onMigrate = {
                navHostController.popBackStack(
                    HomeNavItems.Library.route,
                    inclusive = false
                )
                if (it == MigrationState.ERRORED) {
                    UiToasts.showToast(R.string.migration_error, Toast.LENGTH_LONG)
                    navHostController.navigate("${AnimeInfosRoutes.DETAILS}/${migrateHelperState.oldAnime!!.source}/${migrateHelperState.oldAnime!!.id}")
                } else {
                    navHostController.navigate("${AnimeInfosRoutes.DETAILS}/${migrateHelperState.newAnime!!.source}/${migrateHelperState.newAnime!!.id}")
                }
            },
            onDismissRequest = {
                isMigrationOpened = false
            }
        )
    }
}