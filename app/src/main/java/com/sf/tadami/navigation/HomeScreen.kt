package com.sf.tadami.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sf.tadami.navigation.bottomnav.BottomNavBar
import com.sf.tadami.navigation.graphs.home.HomeNavGraph
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.ui.components.filters.TadaBottomSheetLayout
import com.sf.tadami.ui.tabs.library.bottomsheet.LibrarySheetContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(
    navController: NavHostController,
    homeScreenViewModel: HomeScreenViewModel = viewModel()
) {
    val items = remember {
        listOf(
            HomeNavItems.Library,
            HomeNavItems.Updates,
            HomeNavItems.History,
            HomeNavItems.Browse,
            HomeNavItems.Settings
        )
    }

    val bottomBarHeight by homeScreenViewModel.barHeight.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val displayed = homeScreenViewModel.bottomNavDisplayed

    var manualDisplay by rememberSaveable { mutableStateOf(true) }
    val bottomBarDestination = items.any { it.route == currentDestination?.route }

    val librarySheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    LaunchedEffect(bottomBarDestination, manualDisplay) {
        displayed.targetState = bottomBarDestination && manualDisplay
    }

    TadaBottomSheetLayout(
        sheetState = librarySheetState,
        sheetContent = {
            LibrarySheetContent(sheetState = librarySheetState)
        }
    ) {
        Scaffold(
            bottomBar = {
                AnimatedVisibility(
                    visibleState = displayed,
                    enter = slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight / 2 },
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMedium
                        )
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight },
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                ) {
                    BottomNavBar(
                        modifier = Modifier.onGloballyPositioned {
                            with(density) {
                                val heightDp = it.size.height.toDp()
                                homeScreenViewModel.setBarHeight(heightDp)
                            }
                        },
                        navController = navController,
                        items = items,
                        currentDestination = currentDestination
                    )
                }
            }
        ) {

            HomeNavGraph(
                navController = navController,
                tabsNavPadding = PaddingValues(bottom = if(!manualDisplay && !displayed.currentState) 0.dp else bottomBarHeight),
                bottomNavDisplay = displayed.currentState,
                setNavDisplay = { manualDisplay = it },
                librarySheetVisible =
                librarySheetState.isVisible
                        || librarySheetState.targetValue == ModalBottomSheetValue.Expanded
                        || librarySheetState.targetValue == ModalBottomSheetValue.HalfExpanded,
                showLibrarySheet = {
                    coroutineScope.launch {
                        librarySheetState.show()
                    }
                },
            )
        }
    }
}