package com.sf.tadami.ui.components.material

import androidx.annotation.DrawableRes
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.tv.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.LocalContentColor

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun AnimatedVectorDrawable(
    modifier: Modifier = Modifier,
    @DrawableRes
    animIcon : Int,
    selected: Boolean,
    tint : Color = LocalContentColor.current,
    contentDescription : String = ""
) {
    val image = AnimatedImageVector.animatedVectorResource(animIcon)
    Icon(
        modifier = modifier,
        painter = rememberAnimatedVectorPainter(image, selected),
        contentDescription = contentDescription,
        tint = tint
    )
}