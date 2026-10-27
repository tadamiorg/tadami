package com.sf.tadami.navigation

import com.sf.tadami.ui.utils.LocalAppViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isBackPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sf.tadami.navigation.bottomnav.BottomNavBar
import com.sf.tadami.navigation.bottomnav.NavRail
import com.sf.tadami.navigation.graphs.home.HomeNavGraph
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.ui.components.banners.DownloadedOnlyBanner
import com.sf.tadami.ui.utils.isTabletUi

@Composable
fun HomeScreen(
    navController: NavHostController,
    downloadedOnly: Boolean,
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

    // A rail on the start side instead of the bottom bar
    val tabletUi = isTabletUi()

    // The navigation gives each screen a store of its own, the app keeps this one
    CompositionLocalProvider(LocalAppViewModelStoreOwner provides checkNotNull(LocalViewModelStoreOwner.current)) {
    Scaffold(
        modifier = Modifier.pointerInput(navController) {
            // The back button of the mouse. Android turns it into the back key itself, without a press event
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type == PointerEventType.Press && event.buttons.isBackPressed &&
                        navController.previousBackStackEntry != null
                    ) {
                        navController.navigateUp()
                    }
                }
            }
        },
        topBar = {
            DownloadedOnlyBanner(visible = downloadedOnly)
        },
        bottomBar = {

            if (!tabletUi) AnimatedVisibility(
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

        Row(modifier = Modifier.padding(contentPadding).consumeWindowInsets(contentPadding)) {
            if (tabletUi) {
                AnimatedVisibility(
                    visible = bottomNavVisible,
                    enter = expandHorizontally(
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMedium
                        )
                    ),
                    exit = shrinkHorizontally(
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMedium
                        )
                    ),
                ) {
                    NavRail(
                        navController = navController,
                        items = items,
                        currentDestination = currentDestination
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)){
                HomeNavGraph(
                    navController = navController,
                    setNavDisplay = { manualDisplay = it }
                )
            }
        }

    }
    }
}
