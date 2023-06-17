package com.sf.tadami.ui.components.topappbar

import android.app.Activity
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.sf.tadami.ui.components.data.Action

@Composable
fun ActionItem(
    action: Action,
    enabled: Boolean = action.enabled,
    itemClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    IconButton(
        enabled = enabled,
        onClick = {
            itemClick?.invoke()
            action.onClick()
        }
    ) {
        when (action) {
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
            is Action.CastButton -> {
                AndroidView(
                    factory = {
                        MediaRouteButton(context)
                    },
                    update = {mediaButton ->
                        CastButtonFactory.setUpMediaRouteButton((context as Activity).applicationContext, mediaButton)
                    }
                )
            }
        }

    }
}