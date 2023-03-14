package com.sf.animescraper.ui.animeinfos.details.episodes

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.res.stringResource
import com.sf.animescraper.domain.episode.Episode
import com.sf.animescraper.ui.animeinfos.details.DetailsScreenItem
import com.sf.animescraper.ui.utils.formatMinSec
import com.sf.animescraper.ui.utils.toRelativeString
import java.text.DateFormat
import java.util.*

fun LazyListScope.episodeItems(
    episodes: List<Episode>,
    onEpisodeClicked: (epId: Long) -> Unit,
    onEpisodeLongClick: () -> Unit,
) {
    items(
        items = episodes,
        key = { it.id },
        contentType = { DetailsScreenItem.EPISODE }) { episode ->
        EpisodeListItem(
            title = episode.name,
            onClick = {
                onEpisodeClicked(episode.id)
            },
            onLongClick = onEpisodeLongClick,
            seen = episode.seen,
            watchProgress = episode.timeSeen
                .takeIf { !episode.seen && episode.totalTime > 0L && it > 0L }
                ?.let {
                    "${it.formatMinSec()}/${episode.totalTime.formatMinSec()}"
                },
            date = when {
                episode.dateUpload > 0L -> {
                    Date(episode.dateUpload).toRelativeString()
                }
                episode.dateFetch > 0L -> {
                    Date(episode.dateFetch).toRelativeString()
                }
                else -> null
            },
        )
    }
}