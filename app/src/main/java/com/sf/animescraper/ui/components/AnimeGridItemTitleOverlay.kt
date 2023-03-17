package com.sf.animescraper.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sf.animescraper.ui.utils.verticalGradientBackground

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
                    .padding(8.dp),
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