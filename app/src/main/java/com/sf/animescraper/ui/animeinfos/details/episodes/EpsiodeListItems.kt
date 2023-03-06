package com.sf.animescraper.ui.animeinfos.details.episodes

import androidx.compose.foundation.lazy.LazyListScope
import com.sf.animescraper.network.scraping.dto.details.DetailsEpisode
import com.sf.animescraper.ui.animeinfos.details.DetailsScreenItem

fun LazyListScope.episodeItems(
    episodes: List<DetailsEpisode>,
    onEpisodeClicked: (index : Int) -> Unit
) {

    items(count = episodes.size, key = {episodes[it].url}, contentType = { DetailsScreenItem.EPISODE }){
        EpisodeListItem(
            title = episodes[it].name ?: "",
            onClick = {
                onEpisodeClicked(it)
            },
        )
    }
}