package com.sf.tadami.ui.tabs.browse.filters

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sf.tadami.domain.source.Source
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.ui.components.widgets.FastScrollLazyColumn
import com.sf.tadami.ui.tabs.browse.filters.components.SourcesFilterHeader
import com.sf.tadami.ui.tabs.browse.filters.components.SourcesFilterItem

@Composable
fun SourcesFilterComponent(
    contentPadding: PaddingValues,
    sources: Map<String, MutableList<Source>>
) {

    val sourcesPreferencesState = rememberDataStoreState(SourcesPreferences)
    val sourcesPreferences by sourcesPreferencesState.value.collectAsState()

    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        sources.forEach { (language, sources) ->
            val enabled = language in sourcesPreferences.enabledLanguages
            item(
                key = language,
                contentType = "source-filter-header",
            ) {
                SourcesFilterHeader(
                    language = language,
                    enabled = enabled,
                    onClickItem = {
                        val isEnabled = it in sourcesPreferences.enabledLanguages
                        sourcesPreferencesState.setValue(sourcesPreferences.let { old ->
                            old.copy(
                                enabledLanguages = if (isEnabled) old.enabledLanguages.minus(it) else old.enabledLanguages.plus(
                                    it
                                )
                            )
                        })

                    },
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
                        enabled = "${source.id}" !in sourcesPreferences.hiddenSources,
                        onClickItem = { clickedSource ->
                            val isEnabled = "${clickedSource.id}" in sourcesPreferences.hiddenSources
                            sourcesPreferencesState.setValue(sourcesPreferences.let { old ->
                                old.copy(
                                    hiddenSources = if (isEnabled) old.hiddenSources.minus("${clickedSource.id}") else old.hiddenSources.plus(
                                        "${clickedSource.id}"
                                    )
                                )
                            })
                        },
                    )
                }
            }
        }
    }
}