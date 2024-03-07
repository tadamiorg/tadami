package com.sf.tadami.navigation.drawernav

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.NavigationDrawerItemBorder
import androidx.tv.material3.NavigationDrawerItemColors
import androidx.tv.material3.NavigationDrawerItemDefaults
import androidx.tv.material3.NavigationDrawerItemGlow
import androidx.tv.material3.NavigationDrawerItemScale
import androidx.tv.material3.NavigationDrawerItemShape
import androidx.tv.material3.NavigationDrawerScope

@Composable
fun NavigationDrawerScope.TadaNavigationDrawerItem(
    selected: Boolean,
    onClick: () -> Unit,
    leadingContent: @Composable BoxScope.() -> Unit,
    @SuppressLint("ModifierParameter") leadingContentModifier: Modifier = Modifier.size(NavigationDrawerItemDefaults.IconSize),
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    tonalElevation: Dp = NavigationDrawerItemDefaults.NavigationDrawerItemElevation,
    shape: NavigationDrawerItemShape = NavigationDrawerItemDefaults.shape(),
    colors: NavigationDrawerItemColors = NavigationDrawerItemDefaults.colors(),
    scale: NavigationDrawerItemScale = NavigationDrawerItemScale.None,
    border: NavigationDrawerItemBorder = NavigationDrawerItemDefaults.border(),
    glow: NavigationDrawerItemGlow = NavigationDrawerItemDefaults.glow(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
) {
    val animatedWidth by animateDpAsState(
        targetValue = if (hasFocus) {
            NavigationDrawerItemDefaults.ExpandedDrawerItemWidth
        } else {
            NavigationDrawerItemDefaults.CollapsedDrawerItemWidth
        },
        label = "NavigationDrawerItem width open/closed state of the drawer item"
    )
    val navDrawerItemHeight = if (supportingContent == null) {
        NavigationDrawerItemDefaults.ContainerHeightOneLine
    } else {
        NavigationDrawerItemDefaults.ContainerHeightTwoLine
    }
    ListItem(
        selected = selected,
        onClick = onClick,
        headlineContent = {
            AnimatedVisibility(
                visible = hasFocus,
                enter = NavigationDrawerItemDefaults.ContentAnimationEnter,
                exit = NavigationDrawerItemDefaults.ContentAnimationExit,
            ) {
                content()
            }
        },
        leadingContent = {
            Box(modifier = leadingContentModifier) {
                leadingContent()
            }
        },
        trailingContent = trailingContent?.let {
            {
                AnimatedVisibility(
                    visible = hasFocus,
                    enter = NavigationDrawerItemDefaults.ContentAnimationEnter,
                    exit = NavigationDrawerItemDefaults.ContentAnimationExit,
                ) {
                    it()
                }
            }
        },
        supportingContent = supportingContent?.let {
            {
                AnimatedVisibility(
                    visible = hasFocus,
                    enter = NavigationDrawerItemDefaults.ContentAnimationEnter,
                    exit = NavigationDrawerItemDefaults.ContentAnimationExit,
                ) {
                    it()
                }
            }
        },
        modifier = modifier
            .layout { measurable, constraints ->
                val width = animatedWidth.roundToPx()
                val height = navDrawerItemHeight.roundToPx()
                val placeable = measurable.measure(
                    constraints.copy(
                        minWidth = width,
                        maxWidth = width,
                        minHeight = height,
                        maxHeight = height,
                    )
                )
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            },
        enabled = enabled,
        onLongClick = onLongClick,
        tonalElevation = tonalElevation,
        shape = shape.toToggleableListItemShape(),
        colors = colors.toToggleableListItemColors(hasFocus),
        scale = scale.toToggleableListItemScale(),
        border = border.toToggleableListItemBorder(),
        glow = glow.toToggleableListItemGlow(),
        interactionSource = interactionSource,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavigationDrawerItemShape.toToggleableListItemShape() =
    ListItemDefaults.shape(
        shape = shape,
        focusedShape = focusedShape,
        pressedShape = pressedShape,
        selectedShape = selectedShape,
        disabledShape = disabledShape,
        focusedSelectedShape = focusedSelectedShape,
        focusedDisabledShape = focusedDisabledShape,
        pressedSelectedShape = pressedSelectedShape,
    )

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavigationDrawerItemColors.toToggleableListItemColors(
    doesNavigationDrawerHaveFocus: Boolean
) =
    ListItemDefaults.colors(
        containerColor = containerColor,
        contentColor = if (doesNavigationDrawerHaveFocus) contentColor else inactiveContentColor,
        focusedContainerColor = focusedContainerColor,
        focusedContentColor = focusedContentColor,
        pressedContainerColor = pressedContainerColor,
        pressedContentColor = pressedContentColor,
        selectedContainerColor = selectedContainerColor,
        selectedContentColor = selectedContentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor =
        if (doesNavigationDrawerHaveFocus) disabledContentColor else disabledInactiveContentColor,
        focusedSelectedContainerColor = focusedSelectedContainerColor,
        focusedSelectedContentColor = focusedSelectedContentColor,
        pressedSelectedContainerColor = pressedSelectedContainerColor,
        pressedSelectedContentColor = pressedSelectedContentColor,
    )

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavigationDrawerItemScale.toToggleableListItemScale() =
    ListItemDefaults.scale(
        scale = scale,
        focusedScale = focusedScale,
        pressedScale = pressedScale,
        selectedScale = selectedScale,
        disabledScale = disabledScale,
        focusedSelectedScale = focusedSelectedScale,
        focusedDisabledScale = focusedDisabledScale,
        pressedSelectedScale = pressedSelectedScale,
    )

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavigationDrawerItemBorder.toToggleableListItemBorder() =
    ListItemDefaults.border(
        border = border,
        focusedBorder = focusedBorder,
        pressedBorder = pressedBorder,
        selectedBorder = selectedBorder,
        disabledBorder = disabledBorder,
        focusedSelectedBorder = focusedSelectedBorder,
        focusedDisabledBorder = focusedDisabledBorder,
        pressedSelectedBorder = pressedSelectedBorder,
    )

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavigationDrawerItemGlow.toToggleableListItemGlow() =
    ListItemDefaults.glow(
        glow = glow,
        focusedGlow = focusedGlow,
        pressedGlow = pressedGlow,
        selectedGlow = selectedGlow,
        focusedSelectedGlow = focusedSelectedGlow,
        pressedSelectedGlow = pressedSelectedGlow,
    )
