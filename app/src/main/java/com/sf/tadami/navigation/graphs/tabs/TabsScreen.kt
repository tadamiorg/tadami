package com.sf.tadami.navigation.graphs.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ModalNavigationDrawer
import androidx.tv.material3.rememberDrawerState
import com.sf.tadami.navigation.drawernav.DrawerNavBar

@Composable
fun TabsScreen(
    openAnimeDetails : (sourceId : Long,animeId : Long) -> Unit
) {

    val navController = rememberNavController()

    val items = remember {
        listOf(
            TabsNavItems.Library,
            TabsNavItems.Updates,
            TabsNavItems.History,
            TabsNavItems.Browse,
            TabsNavItems.Settings
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val bottomBarDestination = items.any { it.route == currentDestination?.route }

    var manualDisplay by rememberSaveable { mutableStateOf(true) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val displayed = remember {
        MutableTransitionState(true)
    }

    LaunchedEffect(bottomBarDestination, manualDisplay) {
        displayed.targetState = bottomBarDestination && manualDisplay
    }

    ModalNavigationDrawer(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        drawerState = drawerState,
        drawerContent = { _ ->
            AnimatedVisibility(
                visibleState = displayed,
                enter = slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth / 2 },
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMedium
                    )
                ),
                exit = slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMedium
                    )
                )
            ) {
                DrawerNavBar(
                    items = items,
                    currentDestination = currentDestination,
                    navController = navController
                )
            }
        }
    ) {
        TabsNavGraph(
            navController = navController,
            tabsNavPadding = PaddingValues(start = 56.dp + 24.dp ),
            setNavDisplay = {},
            openAnimeDetails = openAnimeDetails
        )
    }
}