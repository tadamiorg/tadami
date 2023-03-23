package com.sf.tadami.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.sf.tadami.navigation.bottomnav.BottomNavBar
import com.sf.tadami.navigation.graphs.HomeNavGraph
import com.sf.tadami.navigation.graphs.HomeNavItems

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    navLoaded: () -> Unit,
    homeScreenViewModel: HomeScreenViewModel = viewModel()
) {

    val items = remember {
        listOf(
            HomeNavItems.Favorites,
            HomeNavItems.Sources,
            HomeNavItems.Settings
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val displayed = homeScreenViewModel.bottomNavDisplayed

    var manualDisplay by rememberSaveable { mutableStateOf(true) }
    val bottomBarDestination = items.any { it.route == currentDestination?.route }

    LaunchedEffect(bottomBarDestination, manualDisplay) {
        displayed.targetState = bottomBarDestination && manualDisplay
    }

    if (bottomBarDestination) {
        navLoaded()
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visibleState = displayed,
                enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight / 2 }),
                exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight })
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
                setNavDisplay = {
                    manualDisplay = it
                }
            )
        }
    }
}