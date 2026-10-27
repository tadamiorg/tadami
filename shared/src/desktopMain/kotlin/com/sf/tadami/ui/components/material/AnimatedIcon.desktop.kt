package com.sf.tadami.ui.components.material

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color

@Composable
actual fun AnimatedIconImage(
    icon: AnimatedIcon,
    atEnd: Boolean,
    modifier: Modifier,
    tint: Color,
    contentDescription: String,
) {
    val vector = when (icon) {
        AnimatedIcon.VIDEO_LIBRARY -> if (atEnd) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary
        AnimatedIcon.UPDATES -> if (atEnd) Icons.Filled.NewReleases else Icons.Outlined.NewReleases
        AnimatedIcon.HISTORY -> if (atEnd) Icons.Filled.History else Icons.Outlined.History
        AnimatedIcon.SOURCES -> if (atEnd) Icons.Filled.Explore else Icons.Outlined.Explore
        AnimatedIcon.SETTINGS -> Icons.Filled.MoreHoriz
        AnimatedIcon.CARET_DOWN -> Icons.Filled.ExpandMore
    }
    // The caret points up once expanded, where the Android animation ends
    val rotation by animateFloatAsState(if (icon == AnimatedIcon.CARET_DOWN && !atEnd) 180f else 0f)
    Icon(
        imageVector = vector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.rotate(rotation),
    )
}
