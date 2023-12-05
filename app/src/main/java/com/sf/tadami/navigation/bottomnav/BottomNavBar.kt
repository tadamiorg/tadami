package com.sf.tadami.navigation.bottomnav

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.sf.tadami.navigation.graphs.home.HomeNavItems

@Composable
fun BottomNavBar(items : List<HomeNavItems>, currentDestination: NavDestination?, navController: NavHostController) {
    BottomAppBar {
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
    NavigationBarItem(
        icon = {
            Icon(
                painterResource(id = item.icon), contentDescription = stringResource(id = item.name)
            )
        },
        label = {
            Text(stringResource(id = item.name))
        },
        alwaysShowLabel = true,
        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
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