@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.sf.tadami.navigation.drawernav

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ModalNavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.NavigationDrawerItemDefaults
import androidx.tv.material3.NavigationDrawerScope
import androidx.tv.material3.Text
import androidx.tv.material3.rememberDrawerState
import com.sf.tadami.navigation.graphs.tabs.TabsNavItems
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.ui.components.material.AnimatedVectorDrawable
import com.sf.tadami.ui.components.material.BadgeGroup
import com.sf.tadami.ui.components.material.TextBadge


@Composable
fun HomeDrawer(
    items: List<TabsNavItems>,
    currentDestination: NavDestination?,
    navController: NavHostController,
    content: @Composable () -> Unit,
) {
    val closeDrawerWidth = 80.dp
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState, drawerContent = { drawer ->
            Column(
                Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .fillMaxHeight()
                    .padding(12.dp)
                    .selectableGroup(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(
                    8.dp, alignment = Alignment.CenterVertically
                ),
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                items.forEachIndexed { _, item ->
                    val selected by remember(currentDestination?.hierarchy) {
                        derivedStateOf {
                            currentDestination?.hierarchy?.any { it.route == item.route } == true
                        }
                    }
                    NavigationRow(item = item,
                        isSelected = selected,
                        onMenuSelected = {
                            drawerState.setValue(DrawerValue.Closed)
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        })
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }, scrimBrush = Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.surface, Color.Transparent
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = closeDrawerWidth)
        ) {
            content()
        }
    }
}

@Composable
fun NavigationDrawerScope.NavigationRow(
    item: TabsNavItems,
    isSelected: Boolean,
    enabled: Boolean = true,
    onMenuSelected: ((menuItem: TabsNavItems) -> Unit)?
) {
    val libraryPreferences by rememberDataStoreState(customPrefs = LibraryPreferences).value.collectAsState()
    val sourcesPreferences by rememberDataStoreState(customPrefs = SourcesPreferences).value.collectAsState()
    val intSource = remember {
        MutableInteractionSource()
    }

    val focused by intSource.collectIsFocusedAsState()
    TadaNavigationDrawerItem(
        selected = isSelected,
        enabled = enabled,
        interactionSource = intSource,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.5f
            ),
            selectedContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        onClick = {
            onMenuSelected?.invoke(item)
        },
        leadingContent = {
            AnimatedVectorDrawable(
                modifier = Modifier.align(Alignment.Center),
                animIcon = item.icon,
                contentDescription = item.route,
                selected = focused || isSelected
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
                                    color = if (isSelected) {
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
                                    color = if (isSelected) {
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
                                if (isSelected) {
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
                                if (isSelected) {
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
        }
    ) {
        Text(stringResource(id = item.name))
    }
}
