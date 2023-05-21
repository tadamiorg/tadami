package com.sf.tadami.ui.tabs.animesources

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.topappbar.TadaTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeSourcesScreen(navController: NavHostController) {

    val actions = listOf(
        Action.Drawable(
            title = R.string.stub_text,
            icon = R.drawable.ic_search,
            enabled = false,
            onClick = {}),
        Action.CastButton()
    )

    Scaffold(
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

