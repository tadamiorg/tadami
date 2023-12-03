package com.sf.tadami.ui.components.grid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.sf.tadami.ui.utils.padding
import com.sf.tadami.ui.utils.verticalGradientBackground

@Composable
fun BoxScope.AnimeGridItemTitleOverlay(
    title: String
) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .verticalGradientBackground(
                Color.Transparent,
                Color(0xAA000000)
            )
            .fillMaxHeight(0.33f)
            .fillMaxWidth()
            .align(Alignment.BottomCenter),
    ) {
        Row(
            modifier = Modifier.align(Alignment.BottomStart),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(MaterialTheme.padding.extraSmall),
                text = title,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = Color.White,
                    shadow = Shadow(
                        color = Color.Black,
                        blurRadius = 4f,
                    ),
                ),
            )
        }
    }

}