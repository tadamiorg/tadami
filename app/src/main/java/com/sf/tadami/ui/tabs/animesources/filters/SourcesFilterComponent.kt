package com.sf.tadami.ui.tabs.animesources.filters

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sf.tadami.source.online.AnimeCatalogueSource
import com.sf.tadami.ui.components.widgets.FastScrollLazyColumn
import com.sf.tadami.ui.tabs.animesources.filters.components.SourcesFilterHeader
import com.sf.tadami.ui.tabs.animesources.filters.components.SourcesFilterItem
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.preferences.model.rememberDataStoreState

@Composable
fun SourcesFilterComponent(
    contentPadding: PaddingValues,
    sources: Map<String, MutableList<AnimeCatalogueSource>>
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
                        enabled = source.id !in sourcesPreferences.hiddenSources.map { it.toLong() },
                        onClickItem = { source ->
                            val isEnabled = source.id in sourcesPreferences.hiddenSources.map { it.toLong() }
                            sourcesPreferencesState.setValue(sourcesPreferences.let { old ->
                                old.copy(
                                    hiddenSources = if (isEnabled) old.hiddenSources.minus(source.id.toString()) else old.hiddenSources.plus(
                                        source.id.toString()
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