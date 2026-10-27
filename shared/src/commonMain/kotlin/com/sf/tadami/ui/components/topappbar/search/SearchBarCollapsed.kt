package com.sf.tadami.ui.components.topappbar.search

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.topappbar.ActionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarCollapsed(
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    onSearchOpen: () -> Unit,
    searchEnabled : Boolean,
    actions: List<Action> = emptyList()
) {
    val searchActions = remember(actions,searchEnabled) {
        var tempActions = listOf<Action>()
        if(searchEnabled){
            tempActions = tempActions + Action.Drawable(
                title = Res.string.stub_text,
                icon = Res.drawable.ic_search,
                onClick = onSearchOpen
            )
        }
        tempActions = tempActions + actions
        tempActions
    }

    TopAppBar(
        colors = colors,
        title = {
            title()
        },
        actions = {
            searchActions.forEach { action ->
                ActionItem(action = action)
            }
        },
        navigationIcon = navigationIcon
    )
}