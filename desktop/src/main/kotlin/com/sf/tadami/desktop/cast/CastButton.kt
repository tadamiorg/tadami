package com.sf.tadami.desktop.cast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.CastConnected
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.cast_devices_casting
import com.sf.tadami.shared.resources.cast_devices_connecting
import com.sf.tadami.shared.resources.cast_devices_searching
import com.sf.tadami.shared.resources.cast_devices_stop
import com.sf.tadami.shared.resources.cast_devices_title
import com.sf.tadami.shared.resources.ok
import org.jetbrains.compose.resources.stringResource

/** The cast button of the desktop, the MediaRouteButton of the Android app: the devices, or the session. */
@Composable
fun CastButton(tint: Color, modifier: Modifier = Modifier) {
    val session by DesktopCast.session.collectAsState()
    var opened by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { DesktopCast.startDiscovery() }

    IconButton(onClick = { opened = true }, modifier = modifier) {
        Icon(
            imageVector = if (session != null) Icons.Outlined.CastConnected else Icons.Outlined.Cast,
            contentDescription = stringResource(Res.string.cast_devices_title),
            tint = tint,
        )
    }
    if (opened) CastDevicesDialog(onDismiss = { opened = false })
}

@Composable
private fun CastDevicesDialog(onDismiss: () -> Unit) {
    val session by DesktopCast.session.collectAsState()
    val connecting by DesktopCast.connecting.collectAsState()
    val devices by DesktopCast.devices.collectAsState()
    val current = session

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when {
                    current != null -> stringResource(Res.string.cast_devices_casting, current.device.name)
                    else -> stringResource(Res.string.cast_devices_title)
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                when {
                    current != null -> Unit
                    connecting != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(
                            text = stringResource(Res.string.cast_devices_connecting, connecting!!.name),
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                    devices.isEmpty() -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(
                            text = stringResource(Res.string.cast_devices_searching),
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                    else -> devices.forEach { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable { DesktopCast.connect(device) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Tv, contentDescription = null)
                            Column(Modifier.padding(start = 16.dp)) {
                                Text(device.name, style = MaterialTheme.typography.bodyLarge)
                                if (device.model.isNotBlank()) {
                                    Text(device.model, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (current != null) {
                TextButton(onClick = {
                    DesktopCast.disconnect()
                    onDismiss()
                }) { Text(stringResource(Res.string.cast_devices_stop)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.ok)) }
            }
        },
        dismissButton = {
            if (current != null) TextButton(onClick = onDismiss) { Text(stringResource(Res.string.ok)) }
        },
    )
}
