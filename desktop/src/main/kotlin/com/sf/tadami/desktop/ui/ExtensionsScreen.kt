package com.sf.tadami.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sf.tadami.desktop.extension.DEFAULT_EXTENSIONS_REPO
import com.sf.tadami.desktop.extension.ExtensionManager
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun ExtensionsScreen() {
    val manager = remember { Injekt.get<ExtensionManager>() }
    val available by manager.available.collectAsState()
    val installed by manager.installed.collectAsState()
    val busy = remember { mutableStateMapOf<String, Boolean>() }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        runCatching { manager.refreshAvailable() }.onFailure { error = it.toString() }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Extensions", style = MaterialTheme.typography.headlineMedium)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        LazyColumn(contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(available, key = { it.pkg }) { entry ->
                val current = installed.find { it.meta.pkgName == entry.pkg }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(entry.iconUrl(DEFAULT_EXTENSIONS_REPO), null, Modifier.size(40.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entry.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            buildString {
                                append(entry.lang.uppercase()).append(" • ").append(entry.version)
                                current?.let { append(" • installed ").append(it.meta.versionName) }
                                current?.error?.let { append(" • ").append(it) }
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    when {
                        busy[entry.pkg] == true -> CircularProgressIndicator(Modifier.size(24.dp))
                        current == null || current.meta.versionCode < entry.code -> Button(onClick = {
                            busy[entry.pkg] = true
                            scope.launch {
                                runCatching { manager.install(entry) }.onFailure { error = "${entry.displayName}: $it" }
                                busy[entry.pkg] = false
                            }
                        }) { Text(if (current == null) "Install" else "Update") }
                        else -> OutlinedButton(onClick = { manager.uninstall(entry.pkg) }) { Text("Uninstall") }
                    }
                }
            }
        }
    }
}
