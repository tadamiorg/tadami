package com.sf.tadami.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sf.tadami.navigation.bottomnav.BottomNavBar
import com.sf.tadami.navigation.graphs.home.HomeNavGraph
import com.sf.tadami.navigation.graphs.home.HomeNavItems

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(
    navController: NavHostController,
) {
    val items = remember {
        listOf(
            HomeNavItems.Library,
            HomeNavItems.Updates,
            HomeNavItems.History,
            HomeNavItems.Browse,
            HomeNavItems.More
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    var bottomNavVisible by rememberSaveable { mutableStateOf(true) }

    var manualDisplay by rememberSaveable { mutableStateOf(true) }
    val bottomBarDestination = items.any { it.route == currentDestination?.route }

    LaunchedEffect(bottomBarDestination, manualDisplay) {
        bottomNavVisible = bottomBarDestination && manualDisplay
    }

    val scaffoldInsets = WindowInsets(0)

    Scaffold(
        bottomBar = {

            AnimatedVisibility(
                modifier = Modifier.windowInsetsPadding(scaffoldInsets),
                visible = bottomNavVisible,
                enter = expandVertically(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMedium
                    )
                ),
                exit = shrinkVertically(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMedium
                    )
                ),
            ) {
                BottomNavBar(
                    navController = navController,
                    items = items,
                    currentDestination = currentDestination
                )
            }
        },
        contentWindowInsets = scaffoldInsets
    ) { contentPadding ->

        Box(modifier = Modifier.padding(contentPadding).consumeWindowInsets(contentPadding)){
            HomeNavGraph(
                navController = navController,
                setNavDisplay = { manualDisplay = it }
            )
        }

    }
}