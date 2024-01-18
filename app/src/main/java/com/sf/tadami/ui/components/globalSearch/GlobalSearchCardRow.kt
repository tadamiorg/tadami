package com.sf.tadami.ui.components.globalSearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.ui.components.grid.AnimeGridItem
import com.sf.tadami.ui.utils.padding

@Composable
fun GlobalSearchCardRow(
    titles: List<Anime>,
    onClick: (Anime) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(MaterialTheme.padding.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        items(titles) { title ->
            GlobalSearchCard(
                anime = title,
                onClick = { onClick(title) },
            )
        }
    }
}

@Composable
private fun GlobalSearchCard(
    anime: Anime,
    onClick: (Anime) -> Unit,
) {
    Box(modifier = Modifier.width(96.dp)) {
        AnimeGridItem(
            anime = anime,
            titleMaxLines = 2,
            onClick = {
                onClick(anime)
            },
            onLongClick = {
                onClick(anime)
            }
        )
    }
}