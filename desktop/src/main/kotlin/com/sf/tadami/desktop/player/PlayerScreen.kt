package com.sf.tadami.desktop.player

import androidx.compose.foundation.background
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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.data.episode.EpisodeRepository
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.domain.episode.toSEpisode
import com.sf.tadami.extension.util.callGuarded
import com.sf.tadami.source.AnimeCatalogueSource
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.ui.tabs.browse.SourceManager
import com.sf.tadami.ui.utils.AppViewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openani.mediamp.MediaStatus
import org.openani.mediamp.MediampPlayer
import org.openani.mediamp.compose.MediampPlayerSurface
import org.openani.mediamp.compose.rememberMediampPlayer
import org.openani.mediamp.isLoadingOrBuffering
import org.openani.mediamp.source.UriMediaData
import org.openani.mediamp.togglePlayWhenReady
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** The episode and the streams of its source, see [EpisodePlayerScreen]. */
class DesktopPlayerViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val sourceId: Long = checkNotNull(savedStateHandle["sourceId"])
    private val episodeId: Long = checkNotNull(savedStateHandle["episode"])

    private val _anime = MutableStateFlow<Anime?>(null)
    val anime = _anime.asStateFlow()
    private val _episode = MutableStateFlow<Episode?>(null)
    val episode = _episode.asStateFlow()
    private val _streams = MutableStateFlow<List<StreamSource>?>(null)
    val streams = _streams.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val episode = Injekt.get<EpisodeRepository>().getEpisodeById(episodeId)
                _episode.value = episode
                _anime.value = Injekt.get<AnimeRepository>().getAnimeById(episode.animeId)
                val source = Injekt.get<SourceManager>().get(sourceId) as? AnimeCatalogueSource
                    ?: throw IllegalStateException("Source $sourceId isn't installed")
                val streams = source.callGuarded { getEpisodeSources(episode.toSEpisode()) }
                _streams.value = streams
                if (streams.isEmpty()) _error.value = "No stream"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: e.toString()
            }
        }
    }
}

/** The player of the episode of the current screen, mpv drawn in the window. */
@Composable
fun EpisodePlayerScreen(
    onBack: () -> Unit,
    viewModel: DesktopPlayerViewModel = viewModel(factory = AppViewModelFactory),
) {
    val player = rememberMediampPlayer()
    val anime by viewModel.anime.collectAsState()
    val episode by viewModel.episode.collectAsState()
    val streams by viewModel.streams.collectAsState()
    val loadError by viewModel.error.collectAsState()
    var selected by remember(streams) { mutableStateOf(streams?.firstOrNull()) }
    var playError by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }

    LaunchedEffect(selected) {
        val stream = selected ?: return@LaunchedEffect
        playError = null
        // mpv splits its header list on commas: a value with one would become bogus headers
        val headers = stream.headers?.toMultimap()?.mapValues { it.value.last() }?.filterValues { ',' !in it }.orEmpty()
        try {
            player.setMediaData(UriMediaData(stream.url, headers), playWhenReady = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            playError = "${stream.fullName}: $e"
        }
    }

    val state by player.state.collectAsState()
    val position by player.currentPositionMillis.collectAsState()
    val properties by player.mediaProperties.collectAsState()
    val duration = properties?.durationMillis ?: 0L

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        MediampPlayerSurface(player, Modifier.fillMaxSize())
        if (state.isLoadingOrBuffering || streams == null && loadError == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        Row(
            Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.5f)).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { player.stopPlayback(); onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Text(
                listOfNotNull(anime?.title, episode?.name).joinToString(" — "),
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            Box {
                TextButton(onClick = { menu = true }) { Text(selected?.fullName ?: "Streams", color = Color.White) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    streams.orEmpty().forEach { stream ->
                        DropdownMenuItem(text = { Text(stream.fullName) }, onClick = { selected = stream; menu = false })
                    }
                }
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.5f)).padding(8.dp),
        ) {
            (loadError ?: playError ?: player.errorText())?.let {
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
                Text(formatTime(duration), color = Color.White)
            }
        }
    }
}

@Composable
private fun MediampPlayer.errorText(): String? {
    val state by state.collectAsState()
    return (state.mediaStatus as? MediaStatus.Error)?.error?.toString()
}

private fun formatTime(ms: Long): String {
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%02d:%02d".format(s / 60, s % 60)
}
