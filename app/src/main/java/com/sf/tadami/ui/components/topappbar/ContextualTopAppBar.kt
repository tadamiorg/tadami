package com.sf.tadami.ui.components.topappbar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sf.tadami.R
import com.sf.tadami.ui.components.data.Action

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextualTopAppBar(
    modifier: Modifier = Modifier,
    // Non Action Mode
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    colors: TopAppBarColors = topAppBarColors(),
    actions: List<Action> = emptyList(),
    // For Action Mode
    actionModeCounter: Int,
    onCloseActionModeClicked: () -> Unit,
    onToggleAll: () -> Unit,
    onInverseAll: () -> Unit

) {
    val isActionMode by remember(actionModeCounter) {
        derivedStateOf { actionModeCounter > 0 }
    }

    if(isActionMode){
        BackHandler(onBack = onCloseActionModeClicked)
    }

    val generatedActions = if (isActionMode) {
        listOf(
            Action.Vector(
                title = R.string.stub_text,
                icon = Icons.Outlined.SelectAll,
                onClick = onToggleAll
            ),
            Action.Vector(
                title = R.string.stub_text,
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
                    IconButton(onClick = onCloseActionModeClicked) {
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
                TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme
                        .surfaceColorAtElevation(3.dp),
                )
            } else {
                colors
            },
            actions = {
                generatedActions.forEach { action ->
                    ActionItem(action = action)
                }
            }
        )
    }
}