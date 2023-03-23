package com.sf.tadami.ui.tabs.animesources

import android.annotation.SuppressLint
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.sf.tadami.ui.base.widgets.topbar.ScreenTopBar
import com.sf.tadami.ui.components.toolbar.Action
import com.sf.tadami.R

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AnimeSourcesScreen(navController: NavHostController) {

    val actions = listOf(
        Action.Drawable(
            title = R.string.stub_text,
            icon = R.drawable.ic_search,
            onClick = {})
    )
    ScreenTopBar(title = stringResource(id = R.string.sources_tab_title), actions = actions) {
        AnimeSourcesComponent(navController = navController)
    }

}

