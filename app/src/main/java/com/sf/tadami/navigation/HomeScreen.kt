package com.sf.tadami.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sf.tadami.navigation.bottomnav.BottomNavBar
import com.sf.tadami.navigation.graphs.HomeNavGraph
import com.sf.tadami.navigation.graphs.HomeNavItems
import com.sf.tadami.ui.components.filters.TadaBottomSheetLayout
import com.sf.tadami.ui.tabs.library.bottomsheet.LibrarySheetContent
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    navLoaded: () -> Unit,
    homeScreenViewModel: HomeScreenViewModel = viewModel()
) {

    val items = remember {
        listOf(
            HomeNavItems.Library,
            HomeNavItems.Sources,
            HomeNavItems.Settings
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val displayed = homeScreenViewModel.bottomNavDisplayed

    var manualDisplay by rememberSaveable { mutableStateOf(true) }
    val bottomBarDestination = items.any { it.route == currentDestination?.route }

    val librarySheetState  = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val coroutineScope = rememberCoroutineScope()

    if (bottomBarDestination) {
        navLoaded()
    }

    LaunchedEffect(bottomBarDestination, manualDisplay) {
        displayed.targetState = bottomBarDestination && manualDisplay
    }

    TadaBottomSheetLayout(
        sheetState = librarySheetState,
        sheetContent = {
            LibrarySheetContent()
        }
    ) {
        Scaffold(
            bottomBar = {
                AnimatedVisibility(
                    visibleState = displayed,
                    enter = slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight / 2 }, animationSpec = spring(
                            stiffness = Spring.StiffnessMedium,
                            visibilityThreshold = IntOffset.VisibilityThreshold
                        )
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight },
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMedium,
                            visibilityThreshold = IntOffset.VisibilityThreshold
                        )
                    )
                ) {
                    BottomNavBar(
                        navController = navController,
                        items = items,
                        currentDestination = currentDestination
                    )
                }
            }
        ) { bottomPadding ->

            val innerPadding by remember(displayed) {
                derivedStateOf {
                    when {
                        displayed.isIdle && displayed.currentState -> bottomPadding
                        displayed.targetState -> bottomPadding
                        else -> {
                            PaddingValues(0.dp)
                        }
                    }
                }
            }

            Box(modifier = Modifier.padding(innerPadding)) {
                HomeNavGraph(
                    navController = navController,
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
                    }
                )
            }
        }
    }


}