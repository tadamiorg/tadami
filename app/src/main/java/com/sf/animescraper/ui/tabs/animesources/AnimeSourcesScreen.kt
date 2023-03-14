package com.sf.animescraper.ui.tabs.animesources

import android.annotation.SuppressLint
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.sf.animescraper.R
import com.sf.animescraper.ui.base.widgets.topbar.ScreenTopBar
import com.sf.animescraper.ui.components.toolbar.Action


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AnimeSourcesScreen(navController: NavHostController) {

    val actions = listOf(
        Action(title = androidx.appcompat.R.string.search_menu_title, icon = R.drawable.ic_search, onClick = {})
    )
    ScreenTopBar(title = stringResource(id = R.string.sources_tab_title), actions = actions) {
        AnimeSourcesComponent(navController = navController)
    }

}

