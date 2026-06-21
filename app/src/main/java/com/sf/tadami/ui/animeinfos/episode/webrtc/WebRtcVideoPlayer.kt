package com.sf.tadami.ui.animeinfos.episode.webrtc

import android.annotation.SuppressLint
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sf.tadami.R
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.preferences.advanced.AdvancedPreferences
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.ui.animeinfos.episode.EpisodeActivity
import com.sf.tadami.ui.animeinfos.episode.player.PlayerViewModel
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.EpisodesDialog
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.tracksselection.TracksSelectionDialog
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.videoselection.VideoSelectionDialog
import com.sf.tadami.ui.utils.formatMinSec
import java.util.concurrent.atomic.AtomicLong

/**
 * Phone-side controller shown while casting to a Tadami-TV over WebRTC. Mirrors
 * CastVideoPlayer but drives a data-channel [WebRtcSenderSession] instead of a
 * RemoteMediaClient. The TV holds the transport buttons; the phone re-resolves
 * sources/episodes and pushes a new LOAD, and persists watch time from PROGRESS.
 */
@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("ContextCastToActivity")
@Composable
fun WebRtcVideoPlayer(
    sender: WebRtcSender,
    session: WebRtcSenderSession,
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = viewModel(LocalContext.current as EpisodeActivity),
    dispatcher: OnBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher,
) {
    val context = LocalContext.current as EpisodeActivity

    val playerPreferences by rememberDataStoreState(customPrefs = PlayerPreferences).value.collectAsState()
    val advancedPreferences by rememberDataStoreState(customPrefs = AdvancedPreferences).value.collectAsState()

    val episodeUiState by playerViewModel.uiState.collectAsState()
    val currentEpisode by playerViewModel.currentEpisode.collectAsState()
    val episodes by playerViewModel.episodes.collectAsState()
    val anime by playerViewModel.anime.collectAsState()
    val hasNextIterator by playerViewModel.hasNextIterator.collectAsState()
    val hasPreviousIterator by playerViewModel.hasPreviousIterator.collectAsState()

    val outboundId = remember { AtomicLong(1) }

    var currentTime by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }

    var openVideoSelectionDialog by remember { mutableStateOf(false) }
    var openTracksSelectionDialog by remember { mutableStateOf(false) }
    var openEpisodesDialog by remember { mutableStateOf(false) }

    fun updateTime() {
        context.setUpdateTimeJob(
            playerViewModel.updateTime(
                currentEpisode,
                totalDuration,
                currentTime,
                playerPreferences.seenThreshold,
            ),
        )
    }

    fun selectEpisode(episode: Episode) {
        updateTime()
        playerViewModel.setCurrentEpisode(episode)
    }

    // Push a LOAD whenever the resolved source (or episode) changes.
    LaunchedEffect(episodeUiState.selectedSource, currentEpisode?.id) {
        val src = episodeUiState.selectedSource ?: return@LaunchedEffect
        val ep = currentEpisode ?: return@LaunchedEffect
        playerViewModel.getDbEpisodeTime { resumeTime ->
            val selectedSubtitleIndex = episodeUiState.selectedSubtitleTrack
                ?.let { src.subtitleTracks.indexOf(it).takeIf { i -> i >= 0 } }
            session.send(
                Load(
                    id = outboundId.getAndIncrement(),
                    anime = WireAnime(
                        title = anime?.title ?: "",
                        thumbnailUrl = anime?.thumbnailUrl,
                        displayMode = anime?.displayMode.toWireDisplayMode(),
                    ),
                    episode = ep.toWire(),
                    episodes = episodes.map { it.toWire() },
                    selectedSource = src,
                    availableSources = episodeUiState.availableSources,
                    resumeTimeMs = resumeTime.takeIf { it > 0 } ?: 0L,
                    selectedSubtitleIndex = selectedSubtitleIndex,
                    userAgentFallback = advancedPreferences.userAgent,
                    autoplay = true,
                ),
            )
        }
    }

    // Handle messages coming back from the TV.
    LaunchedEffect(session) {
        session.incoming.collect { message ->
            when (message) {
                is Progress -> {
                    currentTime = message.positionMs
                    totalDuration = message.durationMs
                    isPlaying = message.playing
                }

                is SelectSource -> {
                    episodeUiState.availableSources.firstOrNull { it.url == message.sourceUrl }
                        ?.let { playerViewModel.selectSource(it) }
                }

                is SelectEpisode -> {
                    episodes.firstOrNull { it.id == message.episodeId }?.let { selectEpisode(it) }
                }

                is NextEpisode -> {
                    if (hasNextIterator.hasPrevious()) selectEpisode(hasNextIterator.previous())
                }

                is PrevEpisode -> {
                    if (hasPreviousIterator.hasNext()) selectEpisode(hasPreviousIterator.next())
                }

                is SelectSubtitleReq -> {
                    val track = message.subtitleIndex
                        ?.let { episodeUiState.selectedSource?.subtitleTracks?.getOrNull(it) }
                    playerViewModel.selectedSubtitleTrack(track)
                }

                is Ping -> session.send(Pong(outboundId.getAndIncrement()))
                else -> {}
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { updateTime() }
    }

    val episodeNumber = currentEpisode?.episodeNumber

    Box(modifier = modifier.fillMaxSize()) {
        if (episodeUiState.availableSources.isNotEmpty()) {
            VideoSelectionDialog(
                opened = openVideoSelectionDialog,
                sources = episodeUiState.availableSources,
                onSelectSource = {
                    updateTime()
                    playerViewModel.selectSource(it)
                },
                selectedSource = episodeUiState.selectedSource,
                onDismissRequest = { openVideoSelectionDialog = false },
            )
            TracksSelectionDialog(
                opened = openTracksSelectionDialog,
                subtitleTracks = episodeUiState.selectedSource?.subtitleTracks,
                selectedSubtitleTrack = episodeUiState.selectedSubtitleTrack,
                onSubtitleTrackSelected = {
                    playerViewModel.selectedSubtitleTrack(it)
                    val index = it?.let { t -> episodeUiState.selectedSource?.subtitleTracks?.indexOf(t) }
                        ?.takeIf { i -> i >= 0 }
                    session.send(SetSubtitle(outboundId.getAndIncrement(), index))
                },
                onDismissRequest = { openTracksSelectionDialog = false },
            )
        }

        EpisodesDialog(
            opened = openEpisodesDialog,
            onDismissRequest = { openEpisodesDialog = false },
            onConfirm = { selectEpisode(it) },
            displayMode = anime?.displayMode,
            episodes = episodes,
            initialEpisode = currentEpisode,
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = anime?.title ?: "", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = when (anime?.displayMode) {
                    is Anime.DisplayMode.NAME -> currentEpisode?.name
                        ?: "${stringResource(R.string.player_screen_episode_label)} $episodeNumber"

                    else -> "${stringResource(R.string.player_screen_episode_label)} $episodeNumber"
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "${if (isPlaying) "▶" else "⏸"}  ${currentTime.formatMinSec()} / ${totalDuration.formatMinSec()}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(text = "Casting to TV — control playback on your TV", style = MaterialTheme.typography.bodySmall)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (episodeUiState.availableSources.isNotEmpty()) {
                    Button(onClick = { openVideoSelectionDialog = true }) { Text("Source") }
                }
                if (!episodeUiState.selectedSource?.subtitleTracks.isNullOrEmpty()) {
                    Button(onClick = { openTracksSelectionDialog = true }) { Text("Subtitles") }
                }
                Button(onClick = { openEpisodesDialog = true }) { Text("Episodes") }
                OutlinedButton(onClick = {
                    updateTime()
                    sender.disconnect()
                    dispatcher.onBackPressed()
                }) { Text("Stop casting") }
            }
        }
    }
}

private fun Episode.toWire(): WireEpisode = WireEpisode(
    id = id,
    animeId = animeId,
    url = url,
    name = name,
    episodeNumber = episodeNumber,
    seen = seen,
)

private fun Anime.DisplayMode?.toWireDisplayMode(): String = when (this) {
    is Anime.DisplayMode.NAME -> "NAME"
    else -> "NUMBER"
}
