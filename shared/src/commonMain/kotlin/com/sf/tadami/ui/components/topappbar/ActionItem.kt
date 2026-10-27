package com.sf.tadami.ui.components.topappbar

import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.Injekt
import com.sf.tadami.ui.PlatformUi
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.material.DropdownMenu


@Composable
fun ActionItem(
    action: Action,
    enabled: Boolean = action.enabled,
    itemClick: (() -> Unit)? = null,
) {
    val platformUi = remember { Injekt.get<PlatformUi>() }

    when (action) {
        is Action.Drawable -> {
            IconButton(
                enabled = enabled,
                onClick = {
                    itemClick?.invoke()
                    action.onClick()
                }
            ) {
                Icon(
                    painter = painterResource(action.icon),
                    tint = action.tint ?: LocalContentColor.current,
                    contentDescription = stringResource(action.title
                    )
                )
            }
        }

        is Action.Vector -> {

            IconButton(
                enabled = enabled,
                onClick = {
                    itemClick?.invoke()
                    action.onClick()
                }
            ) {
                Icon(
                    imageVector = action.icon,
                    tint = action.tint ?: LocalContentColor.current,
                    contentDescription = stringResource(action.title
                    )
                )
            }
        }

        is Action.CastButton -> if (platformUi.hasCastButton) {
            IconButton(
                enabled = enabled,
                onClick = {
                    itemClick?.invoke()
                    action.onClick()
                },
            ) {
                platformUi.CastButton(tint = LocalContentColor.current)
            }
        }

        is Action.DropDownVector -> {
            var showMenu by remember { mutableStateOf(false) }
            IconButton(
                enabled = enabled,
                onClick = {
                    itemClick?.invoke()
                    showMenu = !showMenu
                }
            ) {
                Icon(
                    imageVector = action.icon,
                    tint = action.tint ?: LocalContentColor.current,
                    contentDescription = stringResource(action.title
                    )
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                action.items.map {
                    DropdownMenuItem(
                        onClick = {
                            it.onClick()
                            showMenu = false
                        },
                        text = { Text(it.title, fontWeight = FontWeight.Normal) },
                        enabled = it.enabled,
                        trailingIcon = it.checked?.let { checked ->
                            { Checkbox(checked = checked, onCheckedChange = null) }
                        },
                    )
                }
            }
        }
        is Action.DropDownDrawable -> {
            var showMenu by remember { mutableStateOf(false) }
            IconButton(
                enabled = enabled,
                onClick = {
                    itemClick?.invoke()
                    showMenu = !showMenu
                }
            ) {
                Icon(
                    painterResource(action.icon),
                    tint = action.tint ?: LocalContentColor.current,
                    contentDescription = stringResource(action.title
                    )
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                action.items.map {
                    DropdownMenuItem(
                        onClick = {
                            it.onClick()
                            showMenu = false
                        },
                        text = { Text(it.title, fontWeight = FontWeight.Normal) },
                        enabled = it.enabled,
                        trailingIcon = it.checked?.let { checked ->
                            { Checkbox(checked = checked, onCheckedChange = null) }
                        },
                    )
                }
            }
        }
    }
}