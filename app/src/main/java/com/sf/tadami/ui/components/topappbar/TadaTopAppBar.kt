package com.sf.tadami.ui.components.topappbar

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sf.tadami.ui.components.data.Action

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TadaTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: List<Action> = emptyList(),
    colors: TopAppBarColors = TopAppBarDefaults.smallTopAppBarColors(),
) {
    TopAppBar(
        modifier = modifier,
        title = {
            ProvideTextStyle(value = MaterialTheme.typography.headlineSmall) {
                title()
            }
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