package com.sf.tadami.ui.animeinfos.episode.webrtc

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions

/**
 * Entry point dialog: discovers Tadami-TV receivers on the LAN and pairs with the
 * code shown on the TV screen.
 */
@Composable
fun WebRtcConnectDialog(
    sender: WebRtcSender,
    onDismissRequest: () -> Unit,
) {
    val devices by sender.devices.collectAsState()
    val status by sender.status.collectAsState()

    var selectedDevice by remember { mutableStateOf<DiscoveredTv?>(null) }
    var code by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        sender.startDiscovery()
        onDispose { sender.stopDiscovery() }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(if (selectedDevice == null) "Cast to TV" else "Enter pairing code") },
        text = {
            val device = selectedDevice
            if (device == null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (devices.isEmpty()) {
                        Text("Searching for Tadami TV on your Wi-Fi…")
                    }
                    LazyColumn {
                        items(devices) { tv ->
                            Text(
                                text = "${tv.name}  (${tv.host})",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedDevice = tv }
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Connecting to ${device.name}. Enter the code shown on the TV.")
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.take(6) },
                        label = { Text("Pairing code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    when (status) {
                        SenderStatus.CONNECTING -> {
                            CircularProgressIndicator()
                        }

                        SenderStatus.PAIR_FAILED -> Text("Wrong code, try again.")
                        SenderStatus.ERROR -> Text("Connection failed.")
                        else -> {}
                    }
                }
            }
        },
        confirmButton = {
            val device = selectedDevice
            if (device != null) {
                TextButton(
                    onClick = { sender.connect(device, code) },
                    enabled = code.length >= 4 && status != SenderStatus.CONNECTING,
                ) { Text("Connect") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        },
    )
}
