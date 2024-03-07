package com.sf.tadami.ui.components.grid

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.ui.animeinfos.details.infos.AnimeCover
import com.sf.tadami.ui.utils.GridSelectedCoverAlpha


@Composable
fun AnimeGridItem(
    anime: Anime,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    titleMaxLines: Int = 2,
    coverAlpha: Float = 1f,
    coverBadgeStart: (@Composable RowScope.() -> Unit)? = null,
    coverBadgeEnd: (@Composable RowScope.() -> Unit)? = null,
) {
    GridItemSelectable(
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        Column {
            AnimeGridCover(
                cover = {
                    AnimeCover.Book(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (isSelected) GridSelectedCoverAlpha else coverAlpha),
                        data = anime.thumbnailUrl,
                    )
                },
                badgesStart = coverBadgeStart,
                badgesEnd = coverBadgeEnd
            )
            GridItemTitle(
                modifier = Modifier.padding(4.dp),
                title = anime.title,
                style = MaterialTheme.typography.titleSmall,
                minLines = 2,
                maxLines = titleMaxLines,
            )
        }
    }
}


@Composable
fun CompactAnimeGridItem(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    anime: Anime,
    onClick: () -> Unit,
    coverAlpha: Float = 1f,
    onLongClick: () -> Unit = onClick,
    coverBadgeStart: @Composable (RowScope.() -> Unit)? = null,
    coverBadgeEnd: @Composable (RowScope.() -> Unit)? = null,
) {
    GridItemSelectable(
        modifier = modifier,
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        AnimeGridCover(
            cover = {
                AnimeCover.Book(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isSelected) GridSelectedCoverAlpha else coverAlpha),
                    data = anime.thumbnailUrl,
                )
            },
            badgesStart = coverBadgeStart,
            badgesEnd = coverBadgeEnd,
            content = {
                CoverTextOverlay(
                    title = anime.title
                )
            },
        )
    }
}

