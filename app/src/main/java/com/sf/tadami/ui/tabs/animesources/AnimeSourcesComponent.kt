package com.sf.tadami.ui.tabs.animesources

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.sf.tadami.navigation.graphs.discover.DiscoverRoutes
import com.sf.tadami.navigation.graphs.sources.SourcesRoutes
import com.sf.tadami.source.online.AnimeCatalogueSource
import com.sf.tadami.ui.components.widgets.ContentLoader
import com.sf.tadami.ui.components.widgets.ScrollbarLazyColumn
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.ui.utils.padding
import com.sf.tadami.utils.Lang
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun AnimeSourcesComponent(
    navController: NavHostController,
    sourcesManager: AnimeSourcesManager = Injekt.get()
) {
    val sourcesPreferences by rememberDataStoreState(SourcesPreferences).value.collectAsState()

    val categories = remember(sourcesPreferences) {
        sourcesManager.animeExtensions.values.toList()
            .fold(mutableMapOf<String, MutableList<AnimeCatalogueSource>>()) { langMap, animeSource ->
                val sourceLang = animeSource.lang.name
                if (sourceLang in sourcesPreferences.enabledLanguages && animeSource.id !in sourcesPreferences.hiddenSources.map { it.toLong() }) {
                    langMap.getOrPut(sourceLang) { mutableListOf() }.add(animeSource)
                }
                langMap
            }
    }
    
    ContentLoader(isLoading = (false)) {
        ScrollbarLazyColumn(
            contentPadding = topSmallPaddingValues,
        ) {
            items(
                items = state.items,
                contentType = {
                    when (it) {
                        is SourceUiModel.Header -> "header"
                        is SourceUiModel.Item -> "item"
                    }
                },
                key = {
                    when (it) {
                        is SourceUiModel.Header -> it.hashCode()
                        is SourceUiModel.Item -> "source-${it.source.key()}"
                    }
                },
            ) { model ->
                when (model) {
                    is SourceUiModel.Header -> {
                        SourceHeader(
                            modifier = Modifier.animateItemPlacement(),
                            language = model.language,
                        )
                    }
                    is SourceUiModel.Item -> SourceItem(
                        modifier = Modifier.animateItemPlacement(),
                        source = model.source,
                        onClickItem = onClickItem,
                        onLongClickItem = onLongClickItem,
                        onClickPin = onClickPin,
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.Center)
    ) {
        LazyColumn {
            categories.keys.toList().forEach { lang ->
                val language = categories[lang]

                item {
                    Text(
                        modifier = Modifier.padding(
                            MaterialTheme.padding.medium,
                            MaterialTheme.padding.tiny
                        ), text = stringResource(id = Lang.getLangByName(lang)!!.getRes())
                    )
                }

                items(language!!.toList()) { source ->

                    AnimeSourceItem(
                        source = source,
                        onRecentClicked = {
                            navController.navigate("${DiscoverRoutes.RECENT}/${source.id}")
                        },
                        onSearchClicked = {
                            navController.navigate("${DiscoverRoutes.SEARCH}/${source.id}")
                        },
                        onOptionsClicked = {
                            navController.navigate("${SourcesRoutes.SETTINGS}/${source.id}")
                        }
                    )

                }
            }
        }
    }
}

/*@Composable
fun AnimeSourcesComponent(
    navController: NavHostController,
    sourcesManager: AnimeSourcesManager = Injekt.get()
) {
    val sourcesPreferences by rememberDataStoreState(SourcesPreferences).value.collectAsState()

    val categories = remember(sourcesPreferences) {
        sourcesManager.animeExtensions.values.toList()
            .fold(mutableMapOf<String, MutableList<AnimeCatalogueSource>>()) { langMap, animeSource ->
                val sourceLang = animeSource.lang.name
                if (sourceLang in sourcesPreferences.enabledLanguages && animeSource.id !in sourcesPreferences.hiddenSources.map { it.toLong() }) {
                    langMap.getOrPut(sourceLang) { mutableListOf() }.add(animeSource)
                }
                langMap
            }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.Center)
    ) {
        LazyColumn {
            categories.keys.toList().forEach { lang ->
                val language = categories[lang]

                item {
                    Text(
                        modifier = Modifier.padding(
                            MaterialTheme.padding.medium,
                            MaterialTheme.padding.tiny
                        ), text = stringResource(id = Lang.getLangByName(lang)!!.getRes())
                    )
                }

                items(language!!.toList()) { source ->

                    AnimeSourceItem(
                        source = source,
                        onRecentClicked = {
                            navController.navigate("${DiscoverRoutes.RECENT}/${source.id}")
                        },
                        onSearchClicked = {
                            navController.navigate("${DiscoverRoutes.SEARCH}/${source.id}")
                        },
                        onOptionsClicked = {
                            navController.navigate("${SourcesRoutes.SETTINGS}/${source.id}")
                        }
                    )

                }
            }
        }
    }
}*/
