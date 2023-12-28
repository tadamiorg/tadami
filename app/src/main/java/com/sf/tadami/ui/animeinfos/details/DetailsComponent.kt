package com.sf.tadami.ui.animeinfos.details

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.ui.animeinfos.details.actions.AnimeActionRow
import com.sf.tadami.ui.animeinfos.details.episodes.EpisodesHeader
import com.sf.tadami.ui.animeinfos.details.episodes.episodeItems
import com.sf.tadami.ui.animeinfos.details.infos.AnimeInfosBox
import com.sf.tadami.ui.animeinfos.details.infos.description.ExpandableAnimeDescription
import com.sf.tadami.ui.components.data.EpisodeItem
import com.sf.tadami.ui.components.widgets.PullRefresh
import com.sf.tadami.ui.components.widgets.VerticalFastScroller

@Composable
fun DetailsComponent(
    isRefreshing : Boolean,
    contentPadding : PaddingValues,
    episodesListState : LazyListState,
    fabHeightInDp : Dp,
    uiState : DetailsUiState,
    sourceName : String,
    isStubSource : Boolean,
    onRefresh : () -> Unit,
    onAddToLibraryClicked : () -> Unit,
    onWebViewClicked : () -> Unit,
    onEpisodeClicked : (Long) -> Unit,
    onEpisodeSelected : (EpisodeItem, Boolean) -> Unit,
    onEpisodeFilterClicked : () -> Unit

) {
    val topPadding = contentPadding.calculateTopPadding()

    val episodes by remember(uiState.episodes,uiState.details?.unseenFilterRaw) {
        derivedStateOf{
            uiState.episodes.addFilters(uiState.details?.unseenFilterRaw)
        }
    }

    PullRefresh(
        refreshing = isRefreshing,
        onRefresh = onRefresh,
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
                        title =  uiState.details?.title ?: "",
                        author = uiState.details?.release,
                        artist = "",
                        status = uiState.details?.status,
                        cover = { uiState.details?.thumbnailUrl ?: "" },
                        sourceName = sourceName,
                        isStubSource = isStubSource
                    )
                }

                item(
                    key = DetailsScreenItem.ACTION_ROW,
                    contentType = DetailsScreenItem.ACTION_ROW,
                ) {
                    AnimeActionRow(
                        favorite = uiState.details?.favorite,
                        onAddToLibraryClicked = onAddToLibraryClicked,
                        onWebViewClicked = onWebViewClicked,
                        onWebViewLongClicked = {}
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
                        episodesNumber = uiState.episodes.size,
                        isFiltered = uiState.details?.areEpisodesFiltered,
                        onFilterClicked = onEpisodeFilterClicked
                    )
                }

                episodeItems(
                    episodes = episodes,
                    displayMode = uiState.details?.displayMode,
                    onEpisodeClicked = onEpisodeClicked,
                    onEpisodeSelected = onEpisodeSelected
                )
            }
        }
    }
}

private fun List<EpisodeItem>.addFilters(unseenFilter: Long?): List<EpisodeItem> {
    return this.filter {
        when(unseenFilter){
            Anime.EPISODE_SHOW_SEEN -> it.episode.seen
            Anime.EPISODE_SHOW_UNSEEN -> !it.episode.seen
            else -> true
        }
    }
}