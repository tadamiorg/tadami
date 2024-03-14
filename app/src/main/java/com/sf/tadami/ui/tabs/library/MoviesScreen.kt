@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.sf.tadami.ui.tabs.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvGridItemSpan
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceBorder
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.ClickableSurfaceShape
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.sf.tadami.domain.anime.toAnime
import com.sf.tadami.ui.animeinfos.details.infos.AnimeCover
import com.sf.tadami.ui.components.data.LibraryItem
import com.sf.tadami.ui.components.grid.CompactAnimeGridItem
import com.sf.tadami.ui.tabs.library.badges.UnseenBadge

@Composable
fun MoviesScreen(modifier: Modifier = Modifier,libraryList: List<LibraryItem>,onItemFocus: (parent: Int, child: Int) -> Unit) {
    MoviesGrid(modifier = modifier, libraryList = libraryList,onItemFocus = onItemFocus)
}

@Composable
fun MoviesGrid(modifier: Modifier, libraryList: List<LibraryItem>, onItemFocus: (parent: Int, child: Int) -> Unit) {
    TvLazyVerticalGrid(
        modifier = modifier,
        columns = TvGridCells.Adaptive(128.dp),
        contentPadding = PaddingValues(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 48.dp),
    ) {
        items(libraryList, key = { it.anime.id }) { libraryItem ->
            VerticalCarouselItem(libraryItem = libraryItem,parent = 0, child = 0, onItemFocus = onItemFocus)
        }
    }
}

@Composable
fun GridHeader() {
    Text(
        text = "Movies",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 24.dp, start = 8.dp),
    )
}

@Composable
fun VerticalCarouselItem(libraryItem: LibraryItem,parent: Int, child: Int, onItemFocus: (parent: Int, child: Int) -> Unit) {
    BorderedFocusableItem(
        onClick = {
            onItemFocus(parent, child)
        },
        modifier = Modifier
            .padding(8.dp)
            .aspectRatio(AnimeCover.Book.ratio),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            CompactAnimeGridItem(
                isSelected = libraryItem.selected,
                anime = libraryItem.anime.toAnime(),
                onClick = {
                },
                onLongClick = {
                },
                coverBadgeStart = {
                    UnseenBadge(count = libraryItem.anime.unseenEpisodes)
                }
            )
        }
    }
}

@Composable
fun BorderedFocusableItem(
    modifier: Modifier = Modifier,
    borderRadius: Dp = 12.dp,
    scale: ClickableSurfaceScale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
    color: ClickableSurfaceColors = ClickableSurfaceDefaults.colors(
        containerColor = MaterialTheme.colorScheme.onSurface,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.surface,
        focusedContentColor = MaterialTheme.colorScheme.onSurface
    ),
    border: ClickableSurfaceBorder = ClickableSurfaceDefaults.border(
        focusedBorder = Border(
            BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(borderRadius)
        )
    ),
    shape: ClickableSurfaceShape = ClickableSurfaceDefaults.shape(
        shape = RoundedCornerShape(borderRadius),
        focusedShape = RoundedCornerShape(borderRadius)
    ),
    onClick: () -> Unit,
    content: @Composable (BoxScope.() -> Unit)
) {
    Surface(
        onClick = { onClick() },
        scale = scale,
        colors = color,
        border = border,
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
    ) {
        content()
    }
}
