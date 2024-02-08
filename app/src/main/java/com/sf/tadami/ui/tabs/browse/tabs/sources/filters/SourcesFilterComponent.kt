package com.sf.tadami.ui.tabs.browse.tabs.sources.filters

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.sf.tadami.domain.source.Source
import com.sf.tadami.ui.components.widgets.ContentLoader
import com.sf.tadami.ui.components.widgets.FastScrollLazyColumn
import com.sf.tadami.ui.tabs.browse.tabs.sources.filters.components.SourcesFilterHeader
import com.sf.tadami.ui.tabs.browse.tabs.sources.filters.components.SourcesFilterItem
import com.sf.tadami.ui.utils.UiToasts

@Composable
fun SourcesFilterComponent(
    contentPadding: PaddingValues,
    uiState: SourcesFilterUiState,
    backHandle: () -> Unit,
    onClickLanguage: (String) -> Unit,
    onClickSource: (Source) -> Unit,
) {

    ContentLoader(isLoading = uiState is SourcesFilterUiState.Loading) {
        if (uiState is SourcesFilterUiState.Error) {
            LaunchedEffect(Unit) {
                UiToasts.showToast("Could not load sources")
                backHandle()
            }
            return@ContentLoader
        }

        val successState = uiState as SourcesFilterUiState.Success
        FastScrollLazyColumn(
            contentPadding = contentPadding,
        ) {
            successState.items.forEach { (language, sources) ->
                val enabled = language in successState.enabledLanguages
                item(
                    key = language,
                    contentType = "source-filter-header",
                ) {
                    SourcesFilterHeader(
                        language = language,
                        enabled = enabled,
                        onClickItem = onClickLanguage,
                    )
                }
                if (enabled) {
                    items(
                        items = sources,
                        key = { "source-filter-${it.id}" },
                        contentType = { "source-filter-item" },
                    ) { source ->
                        SourcesFilterItem(
                            source = source,
                            enabled = "${source.id}" !in successState.hiddenSources,
                            onClickItem = onClickSource,
                        )
                    }
                }
            }
        }
    }
}