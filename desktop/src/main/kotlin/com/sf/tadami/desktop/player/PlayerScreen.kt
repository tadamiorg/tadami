package com.sf.tadami.desktop.player

import com.sf.tadami.desktop.DesktopWindow
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.preferences.advanced.AdvancedPreferences
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.player_epsiode_tooltip_no_content
import com.sf.tadami.shared.resources.player_screen_episode_label
import com.sf.tadami.shared.resources.player_screen_source_load_error
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.ui.animeinfos.episode.player.PlayerViewModel
import com.sf.tadami.ui.animeinfos.episode.player.PlayerViewModelFactory
import com.sf.tadami.ui.animeinfos.episode.player.controls.PlayerControls
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.EpisodesDialog
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.audioselection.AudioSelectionDialog
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.episodetooltip.EpisodeTooltipDialog
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.settings.SettingsDialog
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.tracksselection.TracksSelectionDialog
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.videoselection.VideoSelectionDialog
import com.sf.tadami.ui.components.widgets.ContentLoader
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.utils.convertToIetfLanguageTag
import com.sf.tadami.ui.utils.padding
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.openani.mediamp.MediaStatus
import org.openani.mediamp.MediampPlayer
import org.openani.mediamp.compose.MediampPlayerSurface
import org.openani.mediamp.compose.rememberMediampPlayer
import org.openani.mediamp.features.Buffering
import org.openani.mediamp.features.MediaMetadata
import org.openani.mediamp.isLoadingOrBuffering
import org.openani.mediamp.source.MediaExtraFiles
import org.openani.mediamp.source.Subtitle
import org.openani.mediamp.source.UriMediaData
import java.io.File
import java.net.URI

/**
 * The player of the episode of the current screen, as the Android VideoPlayer: mpv drawn in the window
 * under the shared controls, the history and the resume point kept by the shared PlayerViewModel.
 */
@Composable
fun EpisodePlayerScreen(
    onBack: () -> Unit,
    playerViewModel: PlayerViewModel = viewModel(factory = PlayerViewModelFactory()),
) {
    val player = rememberMediampPlayer()
    val scope = rememberCoroutineScope()

    val playerPreferences by rememberDataStoreState(customPrefs = PlayerPreferences).value.collectAsState()
    val advancedPreferences by rememberDataStoreState(customPrefs = AdvancedPreferences).value.collectAsState()

    val playerScreenLoading by playerViewModel.playerScreenLoading.collectAsState()
    val lockedControls by playerViewModel.lockedControls.collectAsState()
    val playerInitiatedPause by playerViewModel.playerInitiatedPause.collectAsState()
    val episodeUiState by playerViewModel.uiState.collectAsState()
    val currentEpisode by playerViewModel.currentEpisode.collectAsState()
    val episodes by playerViewModel.episodes.collectAsState()
    val isFetchingSources by playerViewModel.isFetchingSources.collectAsState()
    val anime by playerViewModel.anime.collectAsState()
    val hasNextIterator by playerViewModel.hasNextIterator.collectAsState()
    val hasPreviousIterator by playerViewModel.hasPreviousIterator.collectAsState()

    val playerState by player.state.collectAsState()
    val position by player.currentPositionMillis.collectAsState()
    val properties by player.mediaProperties.collectAsState()
    val bufferedPercentage by remember(player) {
        player.features[Buffering]?.bufferedPercentage ?: flowOf(0)
    }.collectAsState(0)
    val totalDuration = properties?.durationMillis?.coerceAtLeast(0L) ?: 0L
    val currentTime = position.coerceIn(0L, totalDuration.takeIf { it > 0 } ?: Long.MAX_VALUE)
    val isPlaying = playerState.isPlaying
    val ended = playerState.mediaStatus is MediaStatus.Ended

    var shouldShowControls by remember { mutableStateOf(true) }
    var openVideoSelectionDialog by remember { mutableStateOf(false) }
    var openTracksSelectionDialog by remember { mutableStateOf(false) }
    var openAudioSelectionDialog by remember { mutableStateOf(false) }
    var openSettingsDialog by remember { mutableStateOf(false) }
    var openEpisodesDialog by remember { mutableStateOf(false) }
    var openEpisodeTooltipDialog by remember { mutableStateOf(false) }

    val updateTime by rememberUpdatedState {
        playerViewModel.updateTime(currentEpisode, totalDuration, currentTime, playerPreferences.seenThreshold)
    }

    fun selectEpisode(episode: Episode) {
        updateTime()
        player.stopPlayback()
        playerViewModel.setCurrentEpisode(episode)
    }

    // Saved before the screen goes: leaving clears the view model, which would cancel the save
    fun leave() {
        val save = updateTime()
        player.stopPlayback()
        scope.launch {
            save?.join()
            onBack()
        }
    }

    fun resumeUnlessPaused() {
        if (!playerInitiatedPause) player.resume()
    }

    LaunchedEffect(episodeUiState.loadError) {
        if (episodeUiState.loadError) leave()
    }

    // A new source starts where the episode was left, its subtitles side-loaded
    LaunchedEffect(episodeUiState.selectedSource) {
        val source = episodeUiState.selectedSource ?: return@LaunchedEffect
        playerViewModel.getDbEpisodeTime { timeSeen ->
            val start = timeSeen.takeIf { it > 0 } ?: currentTime
            scope.launch {
                try {
                    player.setMediaData(mediaData(source, playerViewModel, advancedPreferences.userAgent), true, start)
                } catch (e: Exception) {
                    UiToasts.showToast(stringRes = Res.string.player_screen_source_load_error, args = arrayOf(source.fullName))
                }
            }
        }
    }

    // The subtitle wanted, the manual choice or the preferred language, once mpv knows the tracks
    LaunchedEffect(
        player,
        playerPreferences.subtitlesEnabled,
        playerPreferences.subtitlePrefLanguages,
        episodeUiState.selectedSubtitleTrack,
        episodeUiState.selectedSource,
    ) {
        val tracks = player.features[MediaMetadata]?.subtitleTracks ?: return@LaunchedEffect
        tracks.candidates.collect { candidates ->
            if (!playerPreferences.subtitlesEnabled) {
                tracks.select(null)
                return@collect
            }
            val wanted = episodeUiState.selectedSubtitleTrack?.lang?.convertToIetfLanguageTag()
            val prefs = wanted?.let { listOf(it) } ?: playerPreferences.subtitlePrefLanguages.split(",")
            prefs.firstNotNullOfOrNull { pref ->
                candidates.firstOrNull { it.language.equals(pref, ignoreCase = true) }
            }?.let { tracks.select(it) }
        }
    }

    if (playerPreferences.autoPlay) {
        LaunchedEffect(ended) {
            if (ended && currentTime > 0L && totalDuration > 0L && hasNextIterator.hasPrevious()) {
                selectEpisode(hasNextIterator.previous())
            }
        }
    }

    // The time is saved when the window closes too
    DisposableEffect(Unit) {
        onDispose {
            updateTime()
            DesktopWindow.setFullscreen(false)
        }
    }

    val focusRequester = remember { FocusRequester() }

    ContentLoader(isLoading = playerScreenLoading, delay = 500) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Spacebar -> player.togglePause(playerViewModel)
                        Key.DirectionLeft -> player.seekTo(currentTime - playerPreferences.doubleTapLength)
                        Key.DirectionRight -> player.seekTo(currentTime + playerPreferences.doubleTapLength)
                        Key.F, Key.F11 -> DesktopWindow.setFullscreen(!DesktopWindow.isFullscreen)
                        Key.Escape -> if (DesktopWindow.isFullscreen) DesktopWindow.setFullscreen(false) else return@onPreviewKeyEvent false
                        else -> return@onPreviewKeyEvent false
                    }
                    true
                },
        ) {
            // The keys reach the player once it's there, not while the screen loads
            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            if (episodeUiState.availableSources.isNotEmpty()) {
                VideoSelectionDialog(
                    opened = openVideoSelectionDialog,
                    sources = episodeUiState.availableSources,
                    onSelectSource = {
                        updateTime()
                        playerViewModel.selectSource(it)
                    },
                    selectedSource = episodeUiState.selectedSource,
                    onDismissRequest = {
                        openVideoSelectionDialog = false
                        resumeUnlessPaused()
                    },
                )
                TracksSelectionDialog(
                    opened = openTracksSelectionDialog,
                    subtitleTracks = episodeUiState.selectedSource?.subtitleTracks,
                    selectedSubtitleTrack = episodeUiState.selectedSubtitleTrack,
                    onSubtitleTrackSelected = { playerViewModel.selectedSubtitleTrack(it) },
                    onDismissRequest = {
                        openTracksSelectionDialog = false
                        resumeUnlessPaused()
                    },
                )
                AudioSelectionDialog(
                    opened = openAudioSelectionDialog,
                    audioTracks = episodeUiState.selectedSource?.audioTracks.orEmpty(),
                    selectedAudioTrack = episodeUiState.selectedAudioTrack
                        ?: episodeUiState.selectedSource?.audioTracks?.firstOrNull(),
                    onAudioTrackSelected = { playerViewModel.selectedAudioTrack(it) },
                    onDismissRequest = {
                        openAudioSelectionDialog = false
                        resumeUnlessPaused()
                    },
                )
            }

            EpisodesDialog(
                opened = openEpisodesDialog,
                onDismissRequest = {
                    openEpisodesDialog = false
                    resumeUnlessPaused()
                },
                onConfirm = { selectEpisode(it) },
                displayMode = anime?.displayMode,
                episodes = episodes,
                initialEpisode = currentEpisode,
            )

            SettingsDialog(
                opened = openSettingsDialog,
                onDismissRequest = {
                    openSettingsDialog = false
                    resumeUnlessPaused()
                },
                sourceDatastore = playerViewModel.sourceDataStore,
                sourcePrefsitems = playerViewModel.sourceDataStoreScreen,
            )

            LaunchedEffect(Unit) {
                if (playerViewModel.sourceTooltipAuto == true) openEpisodeTooltipDialog = true
            }

            EpisodeTooltipDialog(
                opened = openEpisodeTooltipDialog,
                onDismissRequest = {
                    openEpisodeTooltipDialog = false
                    resumeUnlessPaused()
                },
                sourceDatastore = playerViewModel.sourceDataStore,
                tooltipContent = playerViewModel.sourceTooltipContent
                    ?: stringResource(Res.string.player_epsiode_tooltip_no_content),
            )

            MediampPlayerSurface(
                player,
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { shouldShowControls = !shouldShowControls })
                    },
            )

            ContentLoader(isLoading = isFetchingSources) {}

            if (playerState.isLoadingOrBuffering && playerState.playWhenReady) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).wrapContentSize(),
                    strokeWidth = 3.dp,
                )
            }

            val episodeLabel = stringResource(Res.string.player_screen_episode_label)
            PlayerControls(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = MaterialTheme.padding.large),
                isVisible = { shouldShowControls },
                isPlaying = isPlaying,
                title = { anime?.title ?: "" },
                episode = when (anime?.displayMode) {
                    is Anime.DisplayMode.NAME -> currentEpisode?.name ?: "$episodeLabel ${currentEpisode?.episodeNumber}"
                    else -> "$episodeLabel ${currentEpisode?.episodeNumber}"
                },
                onReplay = { player.seekTo(currentTime - playerPreferences.doubleTapLength) },
                onSkipOp = { player.seekTo(currentTime + 85000) },
                onForward = { player.seekTo(currentTime + playerPreferences.doubleTapLength) },
                isIdle = ended,
                onTapYoutube = { shouldShowControls = !shouldShowControls },
                playerSeekValue = playerPreferences.doubleTapLength,
                onPauseToggle = { player.togglePause(playerViewModel) },
                onStreamSettings = {
                    player.pause()
                    openVideoSelectionDialog = !openVideoSelectionDialog
                },
                onTracksSettings = {
                    player.pause()
                    openTracksSelectionDialog = !openTracksSelectionDialog
                },
                onAudioSettings = {
                    player.pause()
                    openAudioSelectionDialog = true
                },
                audioSettingsEnabled = episodeUiState.selectedSource?.audioTracks?.isNotEmpty() == true,
                totalDuration = { totalDuration },
                currentTime = { currentTime },
                bufferedPercentage = { bufferedPercentage },
                onSeekChanged = { timeMs: Float -> player.seekTo(timeMs.toLong()) },
                onBack = { leave() },
                onNext = {
                    if (hasNextIterator.hasPrevious()) selectEpisode(hasNextIterator.previous())
                },
                onPrevious = {
                    if (hasPreviousIterator.hasNext()) selectEpisode(hasPreviousIterator.next())
                },
                hasNext = { hasNextIterator.hasPrevious() },
                hasPrevious = { hasPreviousIterator.hasNext() },
                videoSettingsEnabled = episodeUiState.availableSources.isNotEmpty(),
                tracksSettingsEnabled = episodeUiState.selectedSource?.subtitleTracks?.isNotEmpty() == true,
                onEpisodesClicked = {
                    player.pause()
                    openEpisodesDialog = true
                },
                onPlayerSettings = {
                    player.pause()
                    openSettingsDialog = true
                },
                lockedControls = lockedControls,
                onWebViewOpen = { episodeUiState.rawUrl?.let { com.sf.tadami.utils.openInBrowser(it) } },
                episodeTooltipEnabled = playerViewModel.sourceSupportTooltip ?: false,
                onShowEpisodeTooltip = {
                    player.pause()
                    openEpisodeTooltipDialog = !openEpisodeTooltipDialog
                },
                isFullscreen = DesktopWindow.isFullscreen,
                onFullscreenClicked = { DesktopWindow.setFullscreen(!DesktopWindow.isFullscreen) },
            )
        }
    }
}

/** Pauses, plays again, or restarts the episode once ended, as the Android player's toggle. */
private fun MediampPlayer.togglePause(viewModel: PlayerViewModel) {
    val state = state.value
    when {
        state.isPlaying -> {
            viewModel.setPlayerInitadtedPause(true)
            pause()
        }
        state.mediaStatus is MediaStatus.Ended -> {
            seekTo(0)
            resume()
        }
        else -> {
            viewModel.setPlayerInitadtedPause(false)
            resume()
        }
    }
}

/** What mpv plays for [source]: a downloaded file by its path, a stream with its headers and subtitles. */
private fun mediaData(source: StreamSource, viewModel: PlayerViewModel, userAgent: String): UriMediaData {
    val url = source.url
    if (url.startsWith("file:")) return UriMediaData(File(URI(url)).path, emptyMap(), MediaExtraFiles.EMPTY)
    val headers = if (source.headers == null) mapOf("User-Agent" to userAgent) else viewModel.getSourceHeaders()
    val subtitles = source.subtitleTracks
        .filter { it.mimeType != "text/x-unknown" }
        .map { Subtitle(it.url, it.mimeType, it.lang.convertToIetfLanguageTag(), it.lang) }
    // mpv splits its header list on commas: a value with one would become bogus headers
    return UriMediaData(url, headers.filterValues { ',' !in it }, MediaExtraFiles(subtitles))
}
