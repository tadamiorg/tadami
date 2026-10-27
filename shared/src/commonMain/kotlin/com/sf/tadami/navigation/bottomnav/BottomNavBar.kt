package com.sf.tadami.navigation.bottomnav

import com.sf.tadami.ui.components.material.AnimatedIconImage
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.ui.utils.handCursor

@Composable
fun BottomNavBar(
    modifier: Modifier = Modifier,
    items: List<HomeNavItems>,
    currentDestination: NavDestination?,
    navController: NavHostController
) {
    NavigationBar(modifier = modifier) {
        items.forEach { item ->
            val selected = currentDestination.isTab(item)
            NavigationBarItem(
                modifier = Modifier.handCursor(),
                icon = { NavItemIcon(item, selected) },
                label = { NavItemLabel(item) },
                alwaysShowLabel = true,
                selected = selected,
                onClick = { navController.navigateToTab(item) }
            )
        }
    }
}

/** The tablet UI's bottom bar, on the start side */
@Composable
fun NavRail(
    modifier: Modifier = Modifier,
    items: List<HomeNavItems>,
    currentDestination: NavDestination?,
    navController: NavHostController
) {
    NavigationRail(
        modifier = modifier,
        containerColor = NavigationBarDefaults.containerColor,
    ) {
        // The items centered vertically, as Mihon
        Spacer(Modifier.weight(1f))
        items.forEach { item ->
            val selected = currentDestination.isTab(item)
            NavigationRailItem(
                modifier = Modifier.handCursor(),
                icon = { NavItemIcon(item, selected) },
                label = { NavItemLabel(item) },
                alwaysShowLabel = true,
                selected = selected,
                onClick = { navController.navigateToTab(item) }
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

private fun NavDestination?.isTab(item: HomeNavItems): Boolean =
    this?.hierarchy?.any { it.route == item.route } == true

private fun NavHostController.navigateToTab(item: HomeNavItems) {
    navigate(item.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun NavItemIcon(item: HomeNavItems, selected: Boolean) {
    val libraryPreferences by rememberDataStoreState(customPrefs = LibraryPreferences).value.collectAsState()
    val sourcesPreferences by rememberDataStoreState(customPrefs = SourcesPreferences).value.collectAsState()

    BadgedBox(
        badge = {
            when (item.route) {
                HomeNavItems.Browse.route -> {
                    if (sourcesPreferences.extensionUpdatesCount > 0) {
                        Badge {
                            Text(
                                text = sourcesPreferences.extensionUpdatesCount.toString(),
                            )
                        }
                    }
                }

                HomeNavItems.Updates.route -> {
                    if (libraryPreferences.newUpdatesCount > 0) {
                        Badge {
                            Text(
                                text = libraryPreferences.newUpdatesCount.toString(),
                            )
                        }
                    }
                }
            }
        }
    ) {
        AnimatedIconImage(icon = item.icon, atEnd = selected, contentDescription = item.route)
    }
}

@Composable
private fun NavItemLabel(item: HomeNavItems) {
    Text(
        text = stringResource(item.name),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}