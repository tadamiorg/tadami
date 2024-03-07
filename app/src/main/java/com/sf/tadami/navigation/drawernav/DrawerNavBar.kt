package com.sf.tadami.navigation.drawernav

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NavigationDrawerItemDefaults
import androidx.tv.material3.NavigationDrawerScope
import androidx.tv.material3.Text
import com.sf.tadami.navigation.graphs.tabs.TabsNavItems
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.ui.components.material.AnimatedVectorDrawable
import com.sf.tadami.ui.components.material.BadgeGroup
import com.sf.tadami.ui.components.material.TextBadge
import com.sf.tadami.ui.utils.padding

@Composable
fun NavigationDrawerScope.DrawerNavBar(
    modifier: Modifier = Modifier,
    items: List<TabsNavItems>,
    currentDestination: NavDestination?,
    navController: NavHostController
) {
    TvLazyColumn(
        modifier = modifier
            .fillMaxHeight()
            .selectableGroup(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(
            MaterialTheme.padding.extraSmall,
            Alignment.CenterVertically
        )
    ) {
        items(items = items, key = { it.route }) { item ->
            AddItem(item, currentDestination, navController)
        }
    }
}


@Composable
fun NavigationDrawerScope.AddItem(
    item: TabsNavItems,
    currentDestination: NavDestination?,
    navController: NavHostController
) {
    val selected by remember(currentDestination?.hierarchy) {
        derivedStateOf {
            currentDestination?.hierarchy?.any { it.route == item.route } == true
        }
    }

    val libraryPreferences by rememberDataStoreState(customPrefs = LibraryPreferences).value.collectAsState()
    val sourcesPreferences by rememberDataStoreState(customPrefs = SourcesPreferences).value.collectAsState()
    val intSource = remember {
        MutableInteractionSource()
    }

    val focused by intSource.collectIsFocusedAsState()

    TadaNavigationDrawerItem(
        colors = NavigationDrawerItemDefaults.colors(
            focusedSelectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        interactionSource = intSource,
        selected = selected,
        leadingContent = {
            AnimatedVectorDrawable(
                modifier = Modifier.align(Alignment.Center),
                animIcon = item.icon,
                contentDescription = item.route,
                selected = focused || selected
            )
            when (item.route) {
                TabsNavItems.Browse.route -> {
                    if (sourcesPreferences.extensionUpdatesCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    }
                                )
                        )
                    }
                }

                TabsNavItems.Updates.route -> {
                    if (libraryPreferences.newUpdatesCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    }
                                )
                        )
                    }
                }
            }
        },
        content = {
            Text(
                text = stringResource(id = item.name),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            BadgeGroup(
                modifier = Modifier.padding(4.dp)
            ) {

                when (item.route) {
                    TabsNavItems.Browse.route -> {
                        if (sourcesPreferences.extensionUpdatesCount > 0) {
                            TextBadge(
                                text = sourcesPreferences.extensionUpdatesCount.toString(),
                                shape = CircleShape,
                                color =
                                if (selected) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                },
                                padding = PaddingValues(4.dp)
                            )
                        }
                    }

                    TabsNavItems.Updates.route -> {
                        if (libraryPreferences.newUpdatesCount > 0) {
                            TextBadge(
                                text = libraryPreferences.newUpdatesCount.toString(),
                                shape = CircleShape,
                                color =
                                if (selected) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                },
                                padding = PaddingValues(4.dp)
                            )
                        }
                    }
                }
            }
        },
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