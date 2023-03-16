package com.sf.animescraper.ui.components.toolbar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sf.animescraper.ui.base.widgets.topbar.ActionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextualTopAppBar(
    modifier: Modifier = Modifier,
    // Non Action Mode
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.smallTopAppBarColors(),
    actions: List<Action>? = null,
    // For Action Mode
    actionModeCounter: Int,
    onCloseClicked: () -> Unit,
    onToggleAll: () -> Unit,
    onInverseAll: () -> Unit

) {
    val isActionMode by remember(actionModeCounter) {
        derivedStateOf { actionModeCounter > 0 }
    }

    if(isActionMode){
        BackHandler(onBack = onCloseClicked)
    }

    val generatedActions = if (isActionMode) {
        listOf(
            Action.Vector(
                title = androidx.appcompat.R.string.search_menu_title,
                icon = Icons.Outlined.SelectAll,
                onClick = onToggleAll
            ),
            Action.Vector(
                title = androidx.appcompat.R.string.search_menu_title,
                icon = Icons.Outlined.FlipToBack,
                onClick = onInverseAll
            )
        )
    } else {
        actions
    }

    Column(
        modifier = modifier,
    ) {
        TopAppBar(
            title = {
                if (isActionMode) {
                    Text(
                        text = actionModeCounter.toString(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    title()
                }
            },
            navigationIcon = {
                if (isActionMode) {
                    IconButton(onClick = onCloseClicked) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null,
                        )
                    }
                } else {
                    navigationIcon()
                }

            },
            colors = if (isActionMode) {
                TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme
                        .surfaceColorAtElevation(3.dp),
                )
            } else {
                colors
            },
            actions = {
                generatedActions?.forEach { action ->
                    ActionItem(action = action)
                }
            }
        )
    }
}