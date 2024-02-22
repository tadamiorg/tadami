package com.sf.tadami.ui.tabs.browse.tabs.sources.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.domain.source.Source
import com.sf.tadami.ui.components.grid.EmptyScreen
import com.sf.tadami.ui.components.widgets.ContentLoader
import com.sf.tadami.ui.components.widgets.ScrollbarLazyColumn
import com.sf.tadami.ui.tabs.browse.tabs.sources.SourcesUiModel
import com.sf.tadami.ui.tabs.browse.tabs.sources.SourcesUiState
import com.sf.tadami.ui.tabs.browse.tabs.sources.components.item.SourceItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SourcesComponent(
    uiState: SourcesUiState,
    contentPadding: PaddingValues,
    onClickItem: (Source) -> Unit,
    onLongClickItem: (Source) -> Unit = onClickItem,
    onOptionsClicked : (Source) -> Unit = {},
    onRecentClicked: (Source) -> Unit = {},
) {
    ContentLoader(isLoading = uiState.isLoading) {
        when {
            uiState.isEmpty -> EmptyScreen(message = stringResource(id = R.string.src_empty))
            else -> {
                ScrollbarLazyColumn(
                    contentPadding = contentPadding,
                ) {
                    items(
                        items = uiState.items,
                        contentType = {
                            when (it) {
                                is SourcesUiModel.Header -> "header"
                                is SourcesUiModel.Item -> "item"
                            }
                        },
                        key = {
                            when (it) {
                                is SourcesUiModel.Header -> it.hashCode()
                                is SourcesUiModel.Item -> "source-${it.source.key()}"
                            }
                        },
                    ) { model ->
                        when (model) {
                            is SourcesUiModel.Header -> {
                                SourcesHeader(
                                    modifier = Modifier.animateItemPlacement(),
                                    language = model.language,
                                )
                            }
                            is SourcesUiModel.Item -> SourceItem(
                                modifier = Modifier.animateItemPlacement(),
                                source = model.source,
                                onClickItem = onClickItem,
                                onLongClickItem = onLongClickItem,
                                onOptionsClicked = onOptionsClicked,
                                onRecentClicked = onRecentClicked
                            )
                        }
                    }
                }
            }
        }
    }
}