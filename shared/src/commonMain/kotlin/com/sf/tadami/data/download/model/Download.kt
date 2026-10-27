package com.sf.tadami.data.download.model

import com.sf.tadami.data.download.FileProgress
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.source.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One episode of the download queue. */
class Download(
    val source: Source,
    val anime: Anime,
    val episode: Episode,
    /** Its directory name, fixed when queued, see [com.sf.tadami.data.download.DownloadNames] */
    val dirName: String,
) {
    enum class State {
        NOT_DOWNLOADED,
        QUEUED,
        DOWNLOADING,
        DOWNLOADED,
        ERROR,
    }

    private val _statusFlow = MutableStateFlow(State.QUEUED)
    val statusFlow: StateFlow<State> = _statusFlow.asStateFlow()
    var status: State
        get() = _statusFlow.value
        set(value) {
            _statusFlow.value = value
        }

    /** 0 to 100 */
    private val _progressFlow = MutableStateFlow(0)
    val progressFlow: StateFlow<Int> = _progressFlow.asStateFlow()
    var progress: Int
        get() = _progressFlow.value
        set(value) {
            _progressFlow.value = value
        }

    /** The video file's progress: its full size, or its segments for HLS. Null until known. */
    @Volatile
    var videoProgress: FileProgress? = null

    /** Why the download failed, shown to the user */
    @Volatile
    var error: String? = null
}

/** What the episode lists show of an episode's download. */
data class EpisodeDownloadState(
    val state: Download.State = Download.State.NOT_DOWNLOADED,
    val progress: Int = 0,
)
