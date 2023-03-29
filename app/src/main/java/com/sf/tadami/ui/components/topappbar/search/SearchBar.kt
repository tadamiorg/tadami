package com.sf.tadami.ui.components.topappbar.search

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sf.tadami.ui.components.data.Action

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.smallTopAppBarColors(),
    searchOpened: Boolean = false,
    onSearchCancel: () -> Unit,
    onSearchOpen: () -> Unit,
    onSearchChange: (value: String) -> Unit,
    onSearch: (value: String) -> Unit,
    searchValue : String,
    actions: List<Action> = emptyList()
) {

    Column(
        modifier = modifier,
    ) {
        if (searchOpened) {
            SearchBarExpanded(
                colors = colors,
                onSearchCancel = onSearchCancel,
                onSearchChange = onSearchChange,
                onSearch = onSearch,
                actions = actions,
                value = searchValue
            )
        } else {
            SearchBarCollapsed(
                colors = colors,
                title = title,
                navigationIcon = navigationIcon,
                onSearchOpen = onSearchOpen,
                actions = actions
            )
        }
    }
}