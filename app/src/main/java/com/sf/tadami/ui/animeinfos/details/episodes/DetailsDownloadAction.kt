package com.sf.tadami.ui.animeinfos.details.episodes

import com.sf.tadami.data.download.model.Download
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.ui.animeinfos.details.isDisplayed
import com.sf.tadami.ui.components.data.EpisodeItem

/** The download menu of the details screen. */
enum class DetailsDownloadAction(val count: Int? = null) {
    NEXT_1(1),
    NEXT_5(5),
    NEXT_10(10),
    UNSEEN,
    ALL,
}

/**
 * The episodes [action] downloads, in watch order (the list shows the newest first), among those
 * the seen filter displays. The next N are the first unseen ones not downloaded nor queued yet.
 */
internal fun List<EpisodeItem>.episodesToDownload(
    action: DetailsDownloadAction,
    unseenFilter: Long?,
): List<Episode> {
    val displayed = filter { it.isDisplayed(unseenFilter) }.asReversed()
    val unseen = displayed.filter { !it.episode.seen }
    val episodes = when (action) {
        DetailsDownloadAction.ALL -> displayed
        DetailsDownloadAction.UNSEEN -> unseen
        else -> unseen
            .filter { it.download.state == Download.State.NOT_DOWNLOADED }
            .take(action.count!!)
    }
    return episodes.map { it.episode }
}
