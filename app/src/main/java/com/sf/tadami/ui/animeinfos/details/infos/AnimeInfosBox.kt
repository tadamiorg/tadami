package com.sf.tadami.ui.animeinfos.details.infos

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.MaterialTheme
import coil.compose.AsyncImage


@Composable
fun AnimeInfosBox(
    modifier: Modifier = Modifier,
    appBarPadding: Dp,
    title: String,
    author: String?,
    artist: String?,
    cover: () -> String,
    sourceName:String,
    status: String?,
    isStubSource : Boolean
) {
    Box(modifier = modifier) {
        // Backdrop
        val backdropGradientColors = listOf(
            Color.Transparent,
            MaterialTheme.colorScheme.background,
        )
        AsyncImage(
            model = cover(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(colors = backdropGradientColors),
                    )
                }
                .alpha(.2f),
        )

        // Anime & source info
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {

            AnimeAndSourceTitles(
                appBarPadding = appBarPadding,
                cover = cover,
                title = title,
                author = author,
                artist = artist,
                status = status,
                sourceName = sourceName,
                isStubSource = isStubSource
            )

        }
    }
}