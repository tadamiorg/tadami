package com.sf.animescraper.ui.animeinfos.details.episodes

import androidx.compose.foundation.lazy.LazyListScope
import com.sf.animescraper.domain.episode.Episode
import com.sf.animescraper.ui.animeinfos.details.DetailsScreenItem

fun LazyListScope.episodeItems(
    episodes: List<Episode>,
    onEpisodeClicked: (epId : Long) -> Unit
) {

    items(count = episodes.size, key = {episodes[it].id}, contentType = { DetailsScreenItem.EPISODE }){
        EpisodeListItem(
            title = episodes[it].name,
            onClick = {
                onEpisodeClicked(episodes[it].id)
            },
        )
    }
}