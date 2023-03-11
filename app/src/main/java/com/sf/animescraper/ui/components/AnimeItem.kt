package com.sf.animescraper.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sf.animescraper.R
import com.sf.animescraper.domain.anime.Anime

private val CoverPlaceholderColor = Color(0x1F888888)

@Composable
fun AnimeItem(anime: Anime, onAnimeClicked: (anime: Anime) -> Unit) {
    Column(
        modifier = Modifier
            .padding(7.dp, 0.dp, 7.dp, 7.dp)
            .clickable {
                onAnimeClicked(anime)
            },
    ) {

        AsyncImage(
            model = anime.thumbnailUrl,
            placeholder = ColorPainter(CoverPlaceholderColor),
            error = painterResource(R.drawable.cover_error),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f/3f)
                .clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop,
        )

        Text(
            modifier = Modifier
                .sizeIn(minHeight = with(LocalDensity.current) { (MaterialTheme.typography.labelMedium.lineHeight * 3).toDp() })
                .padding(5.dp, 5.dp, 0.dp, 0.dp),
            text = anime.title,
            maxLines = 2,
            style = MaterialTheme.typography.labelMedium,
            overflow = TextOverflow.Ellipsis
        )
    }

}