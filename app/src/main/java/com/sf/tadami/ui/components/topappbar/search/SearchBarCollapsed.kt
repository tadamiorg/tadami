package com.sf.tadami.ui.components.topappbar.search

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.sf.tadami.R
import com.sf.tadami.ui.components.topappbar.ActionItem
import com.sf.tadami.ui.components.data.Action

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarCollapsed(
    colors: TopAppBarColors = TopAppBarDefaults.smallTopAppBarColors(),
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    onSearchOpen: () -> Unit,
    actions: List<Action> = emptyList()
) {
    val searchActions = remember(actions) {
        listOf(
            Action.Drawable(
                title = R.string.stub_text,
                icon = R.drawable.ic_search,
                onClick = onSearchOpen
            )
        ) + actions
    }

    TopAppBar(
        colors = colors,
        title = {
            ProvideTextStyle(value = MaterialTheme.typography.headlineSmall) {
                title()
            }
        },
        actions = {
            searchActions.forEach { action ->
                ActionItem(action = action)
            }
        },
        navigationIcon = navigationIcon
    )
}