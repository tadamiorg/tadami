package com.sf.animescraper.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.sf.animescraper.network.scraping.dto.search.Anime
import com.sf.animescraper.R

@Composable
fun AnimeItem(anime: Anime, onAnimeClicked: (anime: Anime) -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(7.dp, 0.dp, 7.dp, 7.dp)
            .clickable {
                onAnimeClicked(anime)
            },
    ) {

        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(context).data(anime.image).crossfade(true)
                .networkCachePolicy(CachePolicy.ENABLED).build(), contentScale = ContentScale.Crop
        )
        val isImageLoaded = rememberSaveable { mutableStateOf(false) }

        if (painter.state is AsyncImagePainter.State.Success) {
            isImageLoaded.value = true
        }
        Box(
            modifier = Modifier
                .height(150.dp)
                .fillMaxWidth(), contentAlignment = Alignment.Center
        ) {
            if (!isImageLoaded.value) {
                CircularProgressIndicator(modifier = Modifier.zIndex(2f))
            }
            Image(
                modifier = Modifier
                    .matchParentSize()
                    .align(Alignment.Center)
                    .clip(MaterialTheme.shapes.small),
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }

        Text(
            modifier = Modifier
                .sizeIn(minHeight = with(LocalDensity.current) { (MaterialTheme.typography.labelMedium.lineHeight * 3).toDp() })
                .padding(5.dp, 5.dp, 0.dp, 0.dp),
            text = anime.title,
            maxLines = 2,
            style = MaterialTheme.typography.labelMedium,
            overflow = TextOverflow.Ellipsis
        )
        anime.episode?.let { ep ->
            Text(
                modifier = Modifier.padding(5.dp, 0.dp, 0.dp, 0.dp),
                text = "${stringResource(id = R.string.recents_screen_episode_label)} $ep",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }

}