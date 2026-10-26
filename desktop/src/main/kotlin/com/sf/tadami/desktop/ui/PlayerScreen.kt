package com.sf.tadami.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.unit.dp
import com.sf.tadami.source.AnimeCatalogueSource
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.model.SEpisode
import com.sf.tadami.source.model.StreamSource
import kotlinx.coroutines.CancellationException
import org.openani.mediamp.MediaStatus
import org.openani.mediamp.compose.MediampPlayerSurface
import org.openani.mediamp.compose.rememberMediampPlayer
import org.openani.mediamp.isLoadingOrBuffering
import org.openani.mediamp.source.UriMediaData
import org.openani.mediamp.togglePlayWhenReady

@Composable
fun PlayerScreen(source: AnimeCatalogueSource, anime: SAnime, episode: SEpisode, onBack: () -> Unit) {
    val player = rememberMediampPlayer()
    var streams by remember { mutableStateOf<List<StreamSource>?>(null) }
    var selected by remember { mutableStateOf<StreamSource?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }

    LaunchedEffect(episode) {
        runCatching { source.getEpisodeSources(episode) }
            .onSuccess { streams = it; selected = it.firstOrNull(); if (it.isEmpty()) error = "No stream" }
            .onFailure { error = it.toString() }
    }
    LaunchedEffect(selected) {
        val stream = selected ?: return@LaunchedEffect
        error = null
        // mpv splits its header list on commas: a value with one would become bogus headers
        val headers = stream.headers?.toMultimap()?.mapValues { it.value.last() }?.filterValues { ',' !in it }.orEmpty()
        try {
            player.setMediaData(UriMediaData(stream.url, headers), playWhenReady = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = "${stream.fullName}: $e"
        }
    }

    LaunchedEffect(player) {
        while (true) {
            kotlinx.coroutines.delay(2000)
            println("Player: ${player.state.value} at ${player.currentPositionMillis.value} ms of ${player.mediaProperties.value?.durationMillis}, ${selected?.fullName}")
        }
    }
    val state by player.state.collectAsState()
    val position by player.currentPositionMillis.collectAsState()
    val properties by player.mediaProperties.collectAsState()
    val duration = properties?.durationMillis ?: 0L

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        MediampPlayerSurface(player, Modifier.fillMaxSize())
        if (state.isLoadingOrBuffering) CircularProgressIndicator(Modifier.align(Alignment.Center))
        Row(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.5f)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { player.stopPlayback(); onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Text("${anime.title} — ${episode.name}", color = Color.White, modifier = Modifier.weight(1f))
            Box {
                TextButton(onClick = { menu = true }) { Text(selected?.fullName ?: "Streams", color = Color.White) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    streams.orEmpty().forEach { stream ->
                        DropdownMenuItem(text = { Text(stream.fullName) }, onClick = { selected = stream; menu = false })
                    }
                }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.5f)).padding(8.dp)) {
            (error ?: (state.mediaStatus as? MediaStatus.Error)?.error?.toString())?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { player.togglePlayWhenReady() }) {
                    Icon(if (state.playWhenReady) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White)
                }
                Text(formatTime(position), color = Color.White)
                Slider(
                    value = if (duration > 0) position.toFloat() / duration else 0f,
                    onValueChange = { if (duration > 0) player.seekTo((it * duration).toLong()) },
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                Text(formatTime(duration), color = Color.White, modifier = Modifier.clickable { })
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%02d:%02d".format(s / 60, s % 60)
}
