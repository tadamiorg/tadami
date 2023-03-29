package com.sf.tadami.ui.components.topappbar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sf.tadami.R
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.topappbar.search.SearchTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextualSearchTopAppBar(
    modifier: Modifier = Modifier,
    // For Search Mode
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.smallTopAppBarColors(),
    actions: List<Action> = emptyList(),
    isSearchMode: Boolean = false,
    onSearchCancel: (() -> Unit)? = null,
    onSearchChange: ((value: String) -> Unit)? = null,
    onSearchClicked: (() -> Unit)? = null,
    onSearch: ((value: String) -> Unit)? = null,
    backHandlerEnabled : Boolean = true,
    // For Action Mode
    actionModeCounter: Int = 0,
    onCloseActionModeClicked: (() -> Unit)? = null,
    onToggleAll: (() -> Unit)? = null,
    onInverseAll: (() -> Unit)? = null

) {
    val isActionMode by remember(actionModeCounter) {
        derivedStateOf { actionModeCounter > 0 }
    }
    var searchValue by rememberSaveable { mutableStateOf("") }

    when {
        isActionMode -> {
            ActionModeBar(
                modifier = modifier,
                actionModeCounter = actionModeCounter,
                onCloseActionModeClicked = {
                    onCloseActionModeClicked?.invoke()
                },
                onToggleAll = {
                    onToggleAll?.invoke()
                },
                onInverseAll = {
                    onInverseAll?.invoke()
                }
            )
        }
        else -> {
            SearchTopAppBar(
                modifier = modifier,
                searchOpened = isSearchMode,
                colors = colors,
                title = title,
                navigationIcon = navigationIcon,
                onSearchCancel = {
                    onSearchCancel?.invoke()
                    searchValue = ""
                    onSearchChange?.invoke("")
                },
                onSearchOpen = {
                    onSearchClicked?.invoke()
                },
                onSearch = {
                    onSearch?.invoke(it)
                },
                onSearchChange = {
                    searchValue = it
                    onSearchChange?.invoke(it)
                },
                actions = actions,
                searchValue = searchValue,
                backHandlerEnabled = backHandlerEnabled
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionModeBar(
    modifier: Modifier = Modifier,
    actionModeCounter: Int,
    onCloseActionModeClicked: () -> Unit,
    onToggleAll: () -> Unit,
    onInverseAll: () -> Unit
) {
    BackHandler(onBack = onCloseActionModeClicked)

    val actions = remember {
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
    }

    Column(
        modifier = modifier,
    ) {
        TopAppBar(
            title = {
                Text(
                    text = actionModeCounter.toString(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onCloseActionModeClicked) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                    )
                }
            },
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = MaterialTheme.colorScheme
                    .surfaceColorAtElevation(3.dp),
            ),
            actions = {
                actions.forEach { action ->
                    ActionItem(action = action)
                }
            }
        )
    }
}

