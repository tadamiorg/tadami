package com.sf.tadami.ui.tabs.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.discover.DiscoverRoutes
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.topappbar.TadaTopAppBar
import com.sf.tadami.ui.components.topappbar.search.SearchTopAppBar
import com.sf.tadami.ui.tabs.animesources.AnimeSourcesComponent
import com.sf.tadami.ui.tabs.settings.externalpreferences.source.SourcesPreferences
import com.sf.tadami.ui.tabs.settings.model.rememberDataStoreState
import com.sf.tadami.utils.Lang

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavHostController
) {
    val actions = remember {
        listOf(
            Action.Drawable(
                title = R.string.stub_text,
                icon = R.drawable.ic_delete_sweep,
                enabled = true,
                onClick = {

                }),
            Action.CastButton()
        )
    }

    var searchValue by rememberSaveable { mutableStateOf("") }
    var searchOpened by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SearchTopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.label_history))
                },
                searchOpened = searchOpened,
                onSearchOpen = {
                    searchOpened = true
                },
                onSearchChange = {
                    searchValue = it
                },
                onSearch = {

                },
                onSearchCancel = {
                    searchValue = ""
                },
                searchValue = searchValue,
                actions = actions
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            HistoryComponent(navController = navController)
        }
    }
}