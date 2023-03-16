package com.sf.animescraper.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sf.animescraper.ui.base.widgets.topbar.ActionItem
import com.sf.animescraper.ui.components.toolbar.Action

@Composable
fun ContextualBottomBar(
    visible : Boolean,
    actions : List<Action>
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = Alignment.Bottom),
        exit = shrinkVertically(shrinkTowards = Alignment.Bottom)
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background), horizontalArrangement = Arrangement.SpaceEvenly) {
            actions.forEach {action ->
                ActionItem(action)
            }
        }
    }
}