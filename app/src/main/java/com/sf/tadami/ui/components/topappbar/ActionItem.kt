package com.sf.tadami.ui.components.topappbar

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.sf.tadami.ui.components.data.Action

@Composable
fun ActionItem(
    action: Action,
    enabled : Boolean = action.enabled,
    itemClick : (() -> Unit)? = null,
) {
    IconButton(
        enabled = enabled,
        onClick = {
            itemClick?.invoke()
            action.onClick()
        }
    ) {
        when(action){
            is Action.Drawable -> {
                Icon(
                    painter = painterResource(id = action.icon),
                    tint = action.tint ?: LocalContentColor.current,
                    contentDescription = stringResource(
                        id = action.title
                    )
                )
            }
            is Action.Vector -> {
                Icon(
                    imageVector = action.icon,
                    tint = action.tint ?: LocalContentColor.current,
                    contentDescription = stringResource(
                        id = action.title
                    )
                )
            }
        }

    }
}