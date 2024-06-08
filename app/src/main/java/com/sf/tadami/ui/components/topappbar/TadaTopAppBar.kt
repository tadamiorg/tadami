package com.sf.tadami.ui.components.topappbar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sf.tadami.ui.components.data.Action

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TadaTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    actions: List<Action> = emptyList(),
    colors: TopAppBarColors = TopAppBarDefaults.smallTopAppBarColors(),
) {
    TopAppBar(
        modifier = modifier,
        title = {
            title()
        },
        colors = colors,
        navigationIcon = navigationIcon,
        actions = {
            actions.forEach {
                ActionItem(it)
            }
        }
    )
}