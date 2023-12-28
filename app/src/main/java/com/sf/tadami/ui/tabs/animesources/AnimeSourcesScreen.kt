package com.sf.tadami.ui.tabs.animesources

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.discover.DiscoverRoutes
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.topappbar.TadaTopAppBar
import com.sf.tadami.ui.tabs.settings.externalpreferences.source.SourcesPreferences
import com.sf.tadami.ui.tabs.settings.model.rememberDataStoreState
import com.sf.tadami.utils.Lang

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeSourcesScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
    val sPrefs by rememberDataStoreState(customPrefs = SourcesPreferences).value.collectAsState()


    val actions = remember(sPrefs.hiddenSources, sPrefs.enabledLanguages) {
        listOf(
            Action.Drawable(
                title = R.string.stub_text,
                icon = R.drawable.ic_search,
                enabled = true,
                onClick = {
                    navController.navigate(DiscoverRoutes.GLOBAL_SEARCH)
                }),
            Action.Drawable(
                title = R.string.stub_text,
                icon = R.drawable.ic_filter,
                tint = if (sPrefs.hiddenSources.isNotEmpty() || sPrefs.enabledLanguages.size != Lang.getAllLangs().size) Color.Yellow else null,
                enabled = true,
                onClick = {
                    navController.navigate(DiscoverRoutes.SOURCES_FILTER)
                }
            ),
            Action.CastButton()
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TadaTopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.sources_tab_title))
                },
                actions = actions
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            AnimeSourcesComponent(navController = navController)
        }
    }
}

