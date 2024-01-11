package com.sf.tadami.navigation.bottomnav

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.ui.components.material.AnimatedVectorDrawable

@Composable
fun BottomNavBar(items : List<HomeNavItems>, currentDestination: NavDestination?, navController: NavHostController) {
    NavigationBar{
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
    NavigationBarItem(
        icon = {
            BadgedBox(badge = {}) {
                AnimatedVectorDrawable(
                    animIcon = item.icon,
                    contentDescription = item.route,
                    selected = selected
                )
            }
        },
        label = {
            Text(stringResource(id = item.name))
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