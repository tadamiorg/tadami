package com.sf.tadami.ui.components.material

import androidx.annotation.DrawableRes
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun AnimatedVectorDrawable(
    @DrawableRes
    animIcon : Int,
    selected: Boolean,
    tint : Color = LocalContentColor.current,
    contentDescription : String = ""
) {
    val image = AnimatedImageVector.animatedVectorResource(animIcon)
    Icon(
        painter = rememberAnimatedVectorPainter(image, selected),
        contentDescription = contentDescription,
        tint = tint
    )
}