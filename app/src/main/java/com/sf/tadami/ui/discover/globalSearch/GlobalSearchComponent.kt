package com.sf.tadami.ui.discover.globalSearch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.source.AnimeCatalogueSource
import com.sf.tadami.ui.components.globalSearch.GlobalSearchCardRow
import com.sf.tadami.ui.components.globalSearch.GlobalSearchErrorResultItem
import com.sf.tadami.ui.components.globalSearch.GlobalSearchItemResult
import com.sf.tadami.ui.components.globalSearch.GlobalSearchLoadingResultItem
import com.sf.tadami.ui.utils.padding

@Composable
fun GlobalSearchComponent(
    modifier: Modifier = Modifier,
    animesBySource: Map<AnimeCatalogueSource, GlobalSearchItemResult>,
    onAnimeClicked: (anime: Anime) -> Unit,
    onSourceClicked: (AnimeCatalogueSource) -> Unit,
) {
    LazyColumn(modifier = modifier) {
        animesBySource.forEach { (source, result) ->
            item(key = source.id) {
                GlobalSearchResultItem(
                    onClick = { onSourceClicked(source) },
                    title = source.name,
                    subtitle = stringResource(id = source.lang.getRes())
                ) {
                    when (result) {
                        GlobalSearchItemResult.Loading -> {
                            GlobalSearchLoadingResultItem()
                        }
                        is GlobalSearchItemResult.Success -> {
                            if (result.isEmpty) {
                                Text(
                                    text = stringResource(R.string.pager_no_results),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier
                                        .padding(
                                            horizontal = MaterialTheme.padding.medium,
                                            vertical = MaterialTheme.padding.extraSmall,
                                        ),
                                )
                                return@GlobalSearchResultItem
                            }

                            GlobalSearchCardRow(
                                titles = result.result,
                                onClick = onAnimeClicked,
                            )
                        }
                        is GlobalSearchItemResult.Error -> {
                            GlobalSearchErrorResultItem(message = result.throwable.message)
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun GlobalSearchResultItem(
    onClick: () -> Unit,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .padding(
                    start = MaterialTheme.padding.medium,
                    end = MaterialTheme.padding.extraSmall,
                )
                .fillMaxWidth()
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(text = subtitle)
            }
            IconButton(onClick = onClick) {
                Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
            }
        }
        content()
    }
}