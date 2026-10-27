package com.sf.tadami.ui.animeinfos.episode.player.controls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Seekbar preview hooks. With them, dragging the seekbar shows [thumbnail] above the thumb and seeks the
 * player only once, through [onCommit], when the gesture ends. Without them the seekbar seeks as it moves.
 */
@Stable
class SeekBarPreview(
    /** Whether a gesture starting now previews (something plays); asked once per gesture. */
    val isAvailable: () -> Boolean,
    val onScrub: (positionMs: Long) -> Unit,
    val onScrubEnd: () -> Unit,
    val onCommit: (positionMs: Long) -> Unit,
    val thumbnail: @Composable (positionMs: Long) -> Unit,
)
