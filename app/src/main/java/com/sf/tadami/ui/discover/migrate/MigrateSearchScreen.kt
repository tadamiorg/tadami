package com.sf.tadami.ui.discover.migrate

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.animeInfos.AnimeInfosRoutes
import com.sf.tadami.navigation.graphs.discover.DiscoverRoutes
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.ui.discover.globalSearch.GlobalSearchComponent
import com.sf.tadami.ui.discover.globalSearch.GlobalSearchToolbar
import com.sf.tadami.ui.discover.migrate.dialog.MigrateDialog
import com.sf.tadami.ui.discover.migrate.dialog.MigrationState
import com.sf.tadami.ui.utils.UiToasts

@Composable
fun MigrateSearchScreen(
    navController: NavHostController,
    migrateViewModel: MigrateViewModel = viewModel()
) {

    val uiState by migrateViewModel.uiState.collectAsState()
    val helperState by migrateViewModel.helperState.collectAsState()
    var isMigrationOpened by rememberSaveable {
        mutableStateOf(false)
    }

    Box {
        Scaffold(
            topBar = {
                GlobalSearchToolbar(
                    searchValue = uiState.searchQuery,
                    progress = uiState.progress,
                    total = uiState.total,
                    onSearchChange = migrateViewModel::updateSearchQuery,
                    onSearch = {
                        migrateViewModel.search()
                    },
                    onSearchCancel = {
                        migrateViewModel.updateSearchQuery("")
                    }

                )
            },
        ) { paddingValues ->
            GlobalSearchComponent(
                modifier = Modifier.padding(paddingValues),
                animesBySource = uiState.items,
                onAnimeClicked = {
                    migrateViewModel.setClickedAnime(it)
                    isMigrationOpened = true
                },
                onAnimeLongClicked = {
                    navController.navigate("${AnimeInfosRoutes.DETAILS}/${it.source}/${it.id}?migrationId=${migrateViewModel.animeId}")
                },
                onSourceClicked = {
                    navController.navigate("${DiscoverRoutes.SEARCH}/${it.id}?initialQuery=${uiState.searchQuery}&migrationId=${migrateViewModel.animeId}")
                }
            )
        }
        MigrateDialog(
            opened = isMigrationOpened,
            oldAnime = helperState.oldAnime,
            newAnime = helperState.newAnime,
            onClickTitle = {
                if(helperState.newAnime != null){
                    navController.navigate("${AnimeInfosRoutes.DETAILS}/${helperState.newAnime!!.source}/${helperState.newAnime!!.id}?migrationId=${helperState.oldAnime!!.id}")
                }
            },
            onMigrate = {
                navController.popBackStack(
                    HomeNavItems.Library.route,
                    inclusive = false
                )
                if(it == MigrationState.ERRORED){
                    UiToasts.showToast(R.string.migration_error, Toast.LENGTH_LONG)
                    navController.navigate("${AnimeInfosRoutes.DETAILS}/${helperState.oldAnime!!.source}/${helperState.oldAnime!!.id}")
                }
                else{
                    navController.navigate("${AnimeInfosRoutes.DETAILS}/${helperState.newAnime!!.source}/${helperState.newAnime!!.id}")
                }
            },
            onDismissRequest = {
                isMigrationOpened = false
                migrateViewModel.setClickedAnime(null)
            }
        )
    }
}
