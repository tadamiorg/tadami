package com.sf.tadami.navigation.bottomnav

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.ui.components.material.AnimatedVectorDrawable
import com.sf.tadami.ui.tabs.settings.model.rememberDataStoreState
import com.sf.tadami.ui.tabs.settings.screens.library.LibraryPreferences

@Composable
fun BottomNavBar(
    items: List<HomeNavItems>,
    currentDestination: NavDestination?,
    navController: NavHostController
) {
    NavigationBar {
        items.forEach { item ->
            AddItem(item, currentDestination, navController)
        }
    }
}

@Composable
fun RowScope.AddItem(
    item: HomeNavItems,
    currentDestination: NavDestination?,
    navController: NavHostController
) {
    val selected by remember(currentDestination?.hierarchy) {
        derivedStateOf {
            currentDestination?.hierarchy?.any { it.route == item.route } == true
        }
    }

    val libraryPreferences by rememberDataStoreState(customPrefs = LibraryPreferences).value.collectAsState()

    NavigationBarItem(
        icon = {
            BadgedBox(
                badge = {
                    if(libraryPreferences.newUpdatesCount > 0 && item.route == HomeNavItems.Updates.route){
                        Badge {
                            Text(
                                text = libraryPreferences.newUpdatesCount.toString(),
                            )
                        }
                    }
                }
            ) {
                AnimatedVectorDrawable(
                    animIcon = item.icon,
                    contentDescription = item.route,
                    selected = selected
                )
            }
        },
        label = {
            Text(
                text = stringResource(id = item.name),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        alwaysShowLabel = true,
        selected = selected,
        onClick = {
            navController.navigate(item.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    )
}