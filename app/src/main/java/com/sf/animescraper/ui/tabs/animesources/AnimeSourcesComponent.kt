package com.sf.animescraper.ui.tabs.animesources

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sf.animescraper.navigation.graphs.DiscoverRoutes
import com.sf.animescraper.network.api.online.AnimeSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun AnimeSourcesComponent(
    navController: NavHostController,
    sourcesManager: AnimeSourcesManager = Injekt.get()
) {

    val categories = remember {
        sourcesManager.animeExtensions.values.toList()
            .fold(mutableMapOf<Int, MutableList<AnimeSource>>()) { langMap, animeSource ->
                langMap.getOrPut(animeSource.lang.getRes()) { mutableListOf() }.add(animeSource)
                langMap
            }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp,0.dp)
            .wrapContentSize(Alignment.Center)
    ) {
        LazyColumn {
            categories.keys.toList().forEach { lang ->
                val language = categories[lang]

                item { Text(text = stringResource(id = lang)) }

                items(language!!.toList()) { source ->
                    AnimeSourceItem(
                        source = source,
                        onRecentClicked = {
                            navController.navigate("${DiscoverRoutes.RECENT}/${source.id}")
                        },
                        onSearchClicked = {
                            navController.navigate("${DiscoverRoutes.SEARCH}/${source.id}")
                        }
                    )
                }
            }
        }
    }
}