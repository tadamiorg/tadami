package com.sf.tadami.ui.animeinfos.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.sf.tadami.data.download.model.Download
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.coverData
import com.sf.tadami.source.model.SAnimeStatus
import com.sf.tadami.ui.animeinfos.details.actions.AnimeActionRow
import com.sf.tadami.ui.animeinfos.details.episodes.EpisodeDownloadAction
import com.sf.tadami.ui.animeinfos.details.episodes.EpisodesHeader
import com.sf.tadami.ui.animeinfos.details.episodes.episodeItems
import com.sf.tadami.ui.animeinfos.details.infos.AnimeInfosBox
import com.sf.tadami.ui.animeinfos.details.infos.description.ExpandableAnimeDescription
import com.sf.tadami.ui.components.data.EpisodeItem
import com.sf.tadami.ui.components.widgets.CloudflareVerificationLine
import com.sf.tadami.ui.components.widgets.PullRefresh
import com.sf.tadami.ui.components.widgets.TwoPanelBox
import com.sf.tadami.ui.components.widgets.VerticalFastScroller

@Composable
fun DetailsComponent(
    isRefreshing : Boolean,
    contentPadding : PaddingValues,
    episodesListState : LazyListState,
    uiState : DetailsUiState,
    sourceName : String,
    isStubSource : Boolean,
    sourceBaseUrl : String?,
    onRefresh : () -> Unit,
    onAddToLibraryClicked : () -> Unit,
    onEditCategoriesClicked : () -> Unit,
    onWebViewClicked : () -> Unit,
    onEpisodeClicked : (Long) -> Unit,
    onEpisodeSelected : (EpisodeItem, Boolean) -> Unit,
    onEpisodeFilterClicked : () -> Unit,
    onTitleSearch : (String) -> Unit = {},
    onDownloadAction : ((EpisodeItem, EpisodeDownloadAction) -> Unit)? = null,
    // The tablet UI: the anime on the start pane, its episodes on the end one
    twoPane : Boolean = false,

) {
    val topPadding = contentPadding.calculateTopPadding()

    val episodes by remember(uiState.episodes,uiState.details?.unseenFilterRaw,uiState.downloadedOnly) {
        derivedStateOf{
            uiState.episodes.addFilters(uiState.details?.unseenFilterRaw, uiState.downloadedOnly)
        }
    }

    val infoBox = @Composable {
        AnimeInfosBox(
            appBarPadding = topPadding,
            title =  uiState.details?.title ?: "",
            language = uiState.details?.language,
            studio = uiState.details?.studio,
            author = uiState.details?.author,
            release = uiState.details?.release,
            status = uiState.details?.status ?: SAnimeStatus.UNKNOWN,
            cover = { uiState.details?.coverData },
            sourceName = sourceName,
            isStubSource = isStubSource,
            onTitleClicked = {
                uiState.details?.let { onTitleSearch(it.rawTitle ?: it.title) }
            }
        )
    }
    val actionRow = @Composable {
        AnimeActionRow(
            favorite = uiState.details?.favorite,
            onAddToLibraryClicked = onAddToLibraryClicked,
            onEditCategoriesClicked = onEditCategoriesClicked,
            onWebViewClicked = onWebViewClicked,
            onWebViewLongClicked = {}
        )
    }
    val description = @Composable { expanded: Boolean ->
        ExpandableAnimeDescription(
            defaultExpandState = expanded,
            description = uiState.details?.description,
            tagsProvider = { uiState.details?.genres },
        )
    }
    val episodeList: LazyListScope.() -> Unit = {
        item(
            key = DetailsScreenItem.EPISODE_HEADER,
            contentType = DetailsScreenItem.EPISODE_HEADER,
        ) {
            EpisodesHeader(
                totalEpisodes = uiState.episodes.size,
                isFiltered = uiState.details?.areEpisodesFiltered == true || uiState.downloadedOnly,
                onFilterClicked = onEpisodeFilterClicked,
                filteredEpisodes = episodes.size
            )
        }

        episodeItems(
            episodes = episodes,
            displayMode = uiState.details?.displayMode,
            onEpisodeClicked = onEpisodeClicked,
            onEpisodeSelected = onEpisodeSelected,
            onDownloadAction = onDownloadAction
        )
    }

    PullRefresh(
        refreshing = isRefreshing,
        onRefresh = onRefresh,
        indicatorPadding = contentPadding,
        refreshingFooter = {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                CloudflareVerificationLine(
                    sourceBaseUrl,
                    Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                )
            }
        },
    ) {
        val layoutDirection = LocalLayoutDirection.current
        if (twoPane) {
            TwoPanelBox(
                modifier = Modifier.padding(
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    end = contentPadding.calculateEndPadding(layoutDirection),
                ),
                startContent = {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = contentPadding.calculateBottomPadding()),
                    ) {
                        infoBox()
                        actionRow()
                        // The pane has the room for all of it, as Mihon
                        description(true)
                    }
                },
                endContent = {
                    VerticalFastScroller(
                        listState = episodesListState,
                        topContentPadding = topPadding,
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxHeight(),
                            state = episodesListState,
                            contentPadding = PaddingValues(
                                top = topPadding,
                                bottom = contentPadding.calculateBottomPadding(),
                            ),
                            content = episodeList,
                        )
                    }
                },
            )
        } else {
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
                        infoBox()
                    }

                    item(
                        key = DetailsScreenItem.ACTION_ROW,
                        contentType = DetailsScreenItem.ACTION_ROW,
                    ) {
                        actionRow()
                    }

                    item(
                        key = DetailsScreenItem.DESCRIPTION_WITH_TAG,
                        contentType = DetailsScreenItem.DESCRIPTION_WITH_TAG,
                    ) {
                        description(false)
                    }

                    episodeList()
                }
            }
        }
    }
}

private fun List<EpisodeItem>.addFilters(unseenFilter: Long?, downloadedOnly: Boolean): List<EpisodeItem> {
    return this.filter { it.isDisplayed(unseenFilter, downloadedOnly) }
}

public fun EpisodeItem.isDisplayed(unseenFilter: Long?, downloadedOnly: Boolean = false): Boolean {
    // The downloaded only mode forces a downloaded filter, the seen filter still applies
    if (downloadedOnly && download.state != Download.State.DOWNLOADED) return false
    return when(unseenFilter){
        Anime.EPISODE_SHOW_SEEN -> episode.seen
        Anime.EPISODE_SHOW_UNSEEN -> !episode.seen
        else -> true
    }
}