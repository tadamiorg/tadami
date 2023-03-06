package com.sf.animescraper.navigation

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.sf.animescraper.navigation.bottomnav.BottomNavBar
import com.sf.animescraper.navigation.graphs.HomeNavGraph
import com.sf.animescraper.navigation.graphs.HomeNavItems


@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(navController: NavHostController = rememberAnimatedNavController()) {

    val items = listOf(
        HomeNavItems.Favorites,
        HomeNavItems.Sources,
        HomeNavItems.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomBarDestination = items.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = bottomBarDestination,
                enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight / 2 }),
                exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }),
                modifier = Modifier.clickable(
                    enabled = bottomBarDestination,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
            ) {
                BottomNavBar(
                    navController = navController,
                    items = items,
                    currentDestination = currentDestination
                )
            }
        }
    ) { bottomPadding ->

        var innerPadding = bottomPadding

        if (!bottomBarDestination) innerPadding = PaddingValues(0.dp)

        Box(modifier = Modifier.padding(innerPadding)) {
            HomeNavGraph(navController = navController)
        }
    }
}