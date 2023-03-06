package com.sf.animescraper.ui.base.widgets.topbar

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.sf.animescraper.ui.components.toolbar.Action

@Composable
fun ActionItem(
    action: Action
) {
    IconButton(
        enabled = action.enabled,
        onClick = action.onClick
    ) {
        Icon(
            painter = painterResource(id = action.icon),
            contentDescription = stringResource(
                id = action.title
            )
        )
    }
}