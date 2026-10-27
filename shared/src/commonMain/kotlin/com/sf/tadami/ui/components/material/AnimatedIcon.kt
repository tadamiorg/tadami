package com.sf.tadami.ui.components.material

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** The icons animated between two states: the bottom navigation's and the description's caret. */
enum class AnimatedIcon {
    VIDEO_LIBRARY,
    UPDATES,
    HISTORY,
    SOURCES,
    SETTINGS,
    CARET_DOWN,
}

/**
 * [icon] in its end state when [atEnd], animated from one to the other: Android animated vectors,
 * static Material icons on the desktop.
 */
@Composable
expect fun AnimatedIconImage(
    icon: AnimatedIcon,
    atEnd: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    contentDescription: String = "",
)
