package com.sf.animescraper.navigation.bottomnav

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sf.animescraper.R
import com.sf.animescraper.navigation.graphs.HomeNavItems


@Composable
fun BottomNavBar(items : List<HomeNavItems>,currentDestination: NavDestination?,navController: NavHostController) {

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