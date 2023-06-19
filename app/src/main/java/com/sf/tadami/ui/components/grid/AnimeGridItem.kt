package com.sf.tadami.ui.components.grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sf.tadami.R
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.ui.utils.GridSelectedCoverAlpha
import com.sf.tadami.ui.utils.ImageDefaults.CoverPlaceholderColor
import com.sf.tadami.ui.utils.padding
import com.sf.tadami.ui.utils.selectedBorderBackground

@Composable
fun AnimeGridItem(
    anime: Anime,
    modifier: Modifier = Modifier,
    unseenBadge: Long? = null,
    onAnimeClicked: (anime: Anime) -> Unit
) {
    Column(
        modifier = modifier
            .clickable {
                onAnimeClicked(anime)
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
        ) {
            AsyncImage(
                model = anime.thumbnailUrl,
                placeholder = ColorPainter(CoverPlaceholderColor),
                error = painterResource(R.drawable.cover_error),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop,
            )

            if (unseenBadge != null && unseenBadge > 0) {
                AnimeItemBadge(text = unseenBadge.toString())
            }
        }

        Text(
            modifier = Modifier
                .padding(5.dp, 5.dp, 0.dp, 0.dp),
            text = anime.title,
            maxLines = 1,
            style = MaterialTheme.typography.labelMedium,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactAnimeGridItem(
    modifier: Modifier = Modifier,
    isSelected : Boolean = false,
    anime: Anime,
    unseenBadge: Long? = null,
    onClick : () -> Unit,
    onLongClick : () -> Unit = onClick
) {

    Box(modifier = modifier
        .clip(MaterialTheme.shapes.small)
        .combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
        .selectedBorderBackground(isSelected)
        .padding(MaterialTheme.padding.tiny)
    ) {
        AsyncImage(
            model = anime.thumbnailUrl,
            placeholder = ColorPainter(CoverPlaceholderColor),
            error = painterResource(R.drawable.cover_error),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(MaterialTheme.shapes.small)
                .alpha(if (isSelected) GridSelectedCoverAlpha else 1f),
            contentScale = ContentScale.Crop,
        )

        AnimeGridItemTitleOverlay(title = anime.title)

        if (unseenBadge != null && unseenBadge > 0) {
            AnimeItemBadge(text = unseenBadge.toString())
        }
    }
}

