package com.sf.tadami.ui.animeinfos.episode.cast

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Process-wide bus for TV → phone cast state. The receiver broadcasts its current source/subtitle/audio
 * selection over the control channel; [CastControlService] (which owns the channel) publishes it here, and
 * `PlayerViewModel` observes it so the phone's cast UI mirrors changes made on the TV.
 */
object CastRemoteState {

    data class Selection(
        val sourceIndex: Int? = null,
        /** -1 = subtitles off. */
        val subtitleIndex: Int? = null,
        val audioIndex: Int? = null,
    )

    val selection = MutableStateFlow<Selection?>(null)

    fun clear() {
        selection.value = null
    }
}
