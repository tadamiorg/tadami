package com.sf.tadami.ui.animeinfos.episode.player.controls.youtube

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import com.sf.tadami.R
import com.sf.tadami.ui.utils.padding


@Composable
fun SeekAnimation(modifier: Modifier = Modifier,reversed : Boolean = false) {
    val drawableId = R.drawable.ic_play_24 // replace with your vector drawable ID

    val infiniteTransition = rememberInfiniteTransition(label = "")

    val firstImageAlpha = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(0)
        ),
        label = ""
    )

    val secondImageAlpha = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(200)
        ),
        label = ""
    )

    val thirdImageAlpha = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(400)
        ),
        label = ""
    )

    Row(modifier = modifier,horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall)) {
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = "",
            modifier = Modifier.alpha(if(reversed) thirdImageAlpha.value else firstImageAlpha.value).graphicsLayer {
                // Reverse the drawable image horizontally
                if(reversed){
                    scaleX = -1f
                }
            }
        )
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = "",
            modifier = Modifier.alpha(secondImageAlpha.value).graphicsLayer {
                // Reverse the drawable image horizontally
                if(reversed){
                    scaleX = -1f
                }

            }
        )
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = "",
            modifier = Modifier.alpha(if(reversed) firstImageAlpha.value else thirdImageAlpha.value).graphicsLayer {
                // Reverse the drawable image horizontally
                if(reversed){
                    scaleX = -1f
                }
            }
        )
    }
}
