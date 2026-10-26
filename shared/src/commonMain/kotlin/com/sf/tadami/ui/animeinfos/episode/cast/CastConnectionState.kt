package com.sf.tadami.ui.animeinfos.episode.cast

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Process-wide "casting failed to start" flag, observed by both MainActivity and EpisodeActivity.
 *
 * Kept here (not in an Activity field) so the error dialog survives Activity recreation — most
 * importantly a **screen rotation**, where a plain Activity-scoped `mutableStateOf` would be reset
 * to `false` and the dialog would vanish. Set from the Cast [SessionManagerListener] failure
 * callbacks; cleared when the user dismisses the dialog.
 */
object CastConnectionState {
    private val _error = MutableStateFlow(false)
    val error: StateFlow<Boolean> = _error

    fun notifyError() {
        _error.value = true
    }

    fun clear() {
        _error.value = false
    }
}
