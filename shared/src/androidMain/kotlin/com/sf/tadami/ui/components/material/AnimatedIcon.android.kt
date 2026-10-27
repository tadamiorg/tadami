package com.sf.tadami.ui.components.material

import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sf.tadami.shared.R

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
actual fun AnimatedIconImage(
    icon: AnimatedIcon,
    atEnd: Boolean,
    modifier: Modifier,
    tint: Color,
    contentDescription: String,
) {
    val image = AnimatedImageVector.animatedVectorResource(
        when (icon) {
            AnimatedIcon.VIDEO_LIBRARY -> R.drawable.anim_video_library_enter
            AnimatedIcon.UPDATES -> R.drawable.anim_updates_enter
            AnimatedIcon.HISTORY -> R.drawable.anim_history_enter
            AnimatedIcon.SOURCES -> R.drawable.anim_sources_enter
            AnimatedIcon.SETTINGS -> R.drawable.anim_settings_enter
            AnimatedIcon.CARET_DOWN -> R.drawable.anim_caret_down
        },
    )
    Icon(
        painter = rememberAnimatedVectorPainter(image, atEnd),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier,
    )
}
