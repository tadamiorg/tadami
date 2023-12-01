package com.sf.tadami.ui.tabs.animesources.filters

import android.annotation.SuppressLint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.topappbar.TadaTopAppBar
import com.sf.tadami.ui.tabs.animesources.AnimeSourcesManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeSourcesFilerScreen(
    navController: NavHostController,
    sourcesManager: AnimeSourcesManager = Injekt.get()
) {

    Scaffold(
        topBar = {
            TadaTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.sources_tab_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = listOf(
                    Action.CastButton()
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = null,
                        )
                    }
                }
            )
        }
    ) {
        SourcesFilterComponent(
            contentPadding = it,
            sources = remember { sourcesManager.getExtensionsByLanguage() }
        )
    }
}