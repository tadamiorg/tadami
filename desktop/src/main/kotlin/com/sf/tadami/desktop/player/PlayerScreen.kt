package com.sf.tadami.desktop.player

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CastConnected
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sf.tadami.desktop.DesktopWindow
import com.sf.tadami.desktop.DnsProxy
import com.sf.tadami.desktop.cast.DesktopCast
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.preferences.advanced.AdvancedPreferences
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.cast_devices_casting
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
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.openani.mediamp.MediaStatus
import org.openani.mediamp.MediampPlayer
import org.openani.mediamp.compose.MediampPlayerSurface
import org.openani.mediamp.features.Buffering
import org.openani.mediamp.features.MediaMetadata
import org.openani.mediamp.isLoadingOrBuffering
import org.openani.mediamp.mpv.MpvMediampPlayer
import org.openani.mediamp.source.MediaExtraFiles
import org.openani.mediamp.source.Subtitle
import org.openani.mediamp.source.UriMediaData

/**
 * The player of the episode of the current screen, as the Android VideoPlayer: mpv drawn in the window
 * under the shared controls, the history and the resume point kept by the shared PlayerViewModel.
 */
@Composable
fun EpisodePlayerScreen(
    onBack: () -> Unit,
    playerViewModel: PlayerViewModel = viewModel(factory = PlayerViewModelFactory()),
) {
    val player = rememberMpvPlayer()
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
    // While casting, the screen is the TV's remote: its state instead of mpv's
    val castSession by DesktopCast.session.collectAsState()
    val castMedia by remember(castSession) { castSession?.media ?: flowOf(null) }.collectAsState(null)
    val casting = castSession != null

    val totalDuration = if (casting) castMedia?.durationMs ?: 0L else properties?.durationMillis?.coerceAtLeast(0L) ?: 0L
    val currentTime = (if (casting) castMedia?.positionMs ?: 0L else position)
        .coerceIn(0L, totalDuration.takeIf { it > 0 } ?: Long.MAX_VALUE)
    val isPlaying = if (casting) castMedia?.isPlaying == true else playerState.isPlaying
    val ended = if (casting) castMedia?.idleReason == "FINISHED" else playerState.mediaStatus is MediaStatus.Ended

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
        if (playerInitiatedPause) return
        if (casting) DesktopCast.play() else player.resume()
    }

    fun pause() = if (casting) DesktopCast.pause() else player.pause()

    fun seekTo(positionMs: Long) = if (casting) DesktopCast.seekTo(positionMs) else player.seekTo(positionMs)

    fun togglePause() = when {
        !casting -> player.togglePause(playerViewModel)
        isPlaying -> DesktopCast.pause()
        else -> DesktopCast.play()
    }

    LaunchedEffect(episodeUiState.loadError) {
        if (episodeUiState.loadError) leave()
    }

    // A new source starts where the episode was left, its subtitles side-loaded
    // Where the TV was, for the window to go on from there once the cast ends
    var lastCastPosition by remember { mutableLongStateOf(0L) }
    LaunchedEffect(castMedia) {
        castMedia?.positionMs?.takeIf { it > 0 }?.let { lastCastPosition = it }
    }
    var wasCasting by remember { mutableStateOf(false) }
    LaunchedEffect(casting) {
        // The downloaded files again, when the cast needed the streams
        if (wasCasting && !casting) playerViewModel.restoreLocalSource()
        wasCasting = casting
    }

    // The TV switched episode on its own: the screen follows it without loading it again
    LaunchedEffect(castMedia?.episodeId) {
        val media = castMedia ?: return@LaunchedEffect
        val id = media.episodeId ?: return@LaunchedEffect
        if (id == currentEpisode?.id) return@LaunchedEffect
        val episode = episodes.firstOrNull { it.id == id } ?: return@LaunchedEffect
        val (selected, sources) = DesktopCast.castSources(media) ?: return@LaunchedEffect
        playerViewModel.followCastEpisode(episode, episode.url, selected, sources, null)
    }

    // A subtitle picked here, shown on the TV
    LaunchedEffect(castSession) {
        if (castSession == null) return@LaunchedEffect
        snapshotFlow { episodeUiState.selectedSubtitleTrack }.drop(1).collect { track ->
            val index = track?.let { episodeUiState.selectedSource?.subtitleTracks?.indexOf(it) } ?: -1
            DesktopCast.selectSubtitle(index)
        }
    }

    LaunchedEffect(episodeUiState.selectedSource, castSession) {
        val source = episodeUiState.selectedSource ?: return@LaunchedEffect
        val session = castSession
        if (session != null) {
            val episode = currentEpisode ?: return@LaunchedEffect
            // Already on the TV: loaded before, or switched there and followed here
            val media = session.media.value
            if (media != null && media.episodeId == episode.id && DesktopCast.castSources(media)?.first?.url == source.url) {
                return@LaunchedEffect
            }
            // The TV goes on from where the window was
            val localPosition = player.currentPositionMillis.value
            player.stopPlayback()
            playerViewModel.getDbEpisodeTime { timeSeen ->
                scope.launch {
                    val start = localPosition.takeIf { it > 0 } ?: timeSeen.coerceAtLeast(0L)
                    // Downloaded files the proxy can't serve: the source's streams, as on Android
                    if (!DesktopCast.loadEpisode(episode, anime, source, episodeUiState.availableSources, episodes, start)) {
                        playerViewModel.fetchOnlineSources()
                    }
                }
            }
            return@LaunchedEffect
        }
        playerViewModel.getDbEpisodeTime { timeSeen ->
            val start = lastCastPosition.takeIf { it > 0 } ?: timeSeen.takeIf { it > 0 } ?: currentTime
            lastCastPosition = 0L
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

    // The TV goes to the next episode itself
    if (playerPreferences.autoPlay && !casting) {
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

    // The controls come with the mouse or a key, and go once neither moved for a while during playback
    var activity by remember { mutableIntStateOf(0) }
    fun showControls() {
        shouldShowControls = true
        activity++
    }
    val dialogOpen = openVideoSelectionDialog || openTracksSelectionDialog || openAudioSelectionDialog ||
        openSettingsDialog || openEpisodesDialog || openEpisodeTooltipDialog
    LaunchedEffect(activity, shouldShowControls, isPlaying, dialogOpen) {
        if (shouldShowControls && isPlaying && !dialogOpen) {
            delay(CONTROLS_HIDE_DELAY)
            shouldShowControls = false
        }
    }

    // The window's keys, whatever has the focus, as in a video player
    val onKey by rememberUpdatedState { event: KeyEvent ->
        if (event.type != KeyEventType.KeyDown || dialogOpen) return@rememberUpdatedState false
        when {
            event.key == Key.Spacebar || event.key == Key.K -> togglePause()
            event.key == Key.DirectionLeft || event.key == Key.J -> seekTo(currentTime - playerPreferences.doubleTapLength)
            event.key == Key.DirectionRight || event.key == Key.L -> seekTo(currentTime + playerPreferences.doubleTapLength)
            event.isShiftPressed && event.key == Key.N -> if (hasNextIterator.hasPrevious()) selectEpisode(hasNextIterator.previous())
            event.isShiftPressed && event.key == Key.P -> if (hasPreviousIterator.hasNext()) selectEpisode(hasPreviousIterator.next())
            event.key == Key.F || event.key == Key.F11 -> DesktopWindow.setFullscreen(!DesktopWindow.isFullscreen)
            event.key == Key.Escape && DesktopWindow.isFullscreen -> DesktopWindow.setFullscreen(false)
            else -> return@rememberUpdatedState false
        }
        showControls()
        true
    }
    DisposableEffect(Unit) {
        val handler: (KeyEvent) -> Boolean = { onKey(it) }
        DesktopWindow.keyHandler = handler
        onDispose { if (DesktopWindow.keyHandler === handler) DesktopWindow.keyHandler = null }
    }

    ContentLoader(isLoading = playerScreenLoading, delay = 500) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            when (event.type) {
                                PointerEventType.Move, PointerEventType.Enter, PointerEventType.Scroll -> showControls()
                                PointerEventType.Exit -> if (isPlaying) shouldShowControls = false
                            }
                        }
                    }
                }
                // No cursor over the picture while the controls are away
                .pointerHoverIcon(if (shouldShowControls) PointerIcon.Default else HiddenCursor),
        ) {

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

            if (casting) {
                CastingPanel(castSession?.device?.name.orEmpty(), Modifier.align(Alignment.Center))
            } else MediampPlayerSurface(
                player,
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { showControls() })
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
                onReplay = { seekTo(currentTime - playerPreferences.doubleTapLength) },
                onSkipOp = { seekTo(currentTime + 85000) },
                onForward = { seekTo(currentTime + playerPreferences.doubleTapLength) },
                isIdle = ended,
                onTapYoutube = { showControls() },
                playerSeekValue = playerPreferences.doubleTapLength,
                onPauseToggle = { togglePause() },
                onStreamSettings = {
                    pause()
                    openVideoSelectionDialog = !openVideoSelectionDialog
                },
                onTracksSettings = {
                    pause()
                    openTracksSelectionDialog = !openTracksSelectionDialog
                },
                onAudioSettings = {
                    pause()
                    openAudioSelectionDialog = true
                },
                audioSettingsEnabled = episodeUiState.selectedSource?.audioTracks?.isNotEmpty() == true,
                totalDuration = { totalDuration },
                currentTime = { currentTime },
                bufferedPercentage = { bufferedPercentage },
                onSeekChanged = { timeMs: Float -> seekTo(timeMs.toLong()) },
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
                    pause()
                    openEpisodesDialog = true
                },
                onPlayerSettings = {
                    pause()
                    openSettingsDialog = true
                },
                lockedControls = lockedControls,
                onWebViewOpen = { episodeUiState.rawUrl?.let { com.sf.tadami.utils.openInBrowser(it) } },
                episodeTooltipEnabled = playerViewModel.sourceSupportTooltip ?: false,
                onShowEpisodeTooltip = {
                    pause()
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
    // The app's user agent unless the source has its own, as OkHttp sends it on Android: hosts refuse mpv's
    val sourceHeaders = if (source.headers == null) emptyMap() else viewModel.getSourceHeaders()
    val headers = if (sourceHeaders.keys.any { it.equals("User-Agent", ignoreCase = true) }) {
        sourceHeaders
    } else {
        mapOf("User-Agent" to userAgent) + sourceHeaders
    }
    val subtitles = source.subtitleTracks
        .filter { it.mimeType != "text/x-unknown" }
        .map { Subtitle(it.url, it.mimeType, it.lang.convertToIetfLanguageTag(), it.lang) }
    // Mediamp gives mpv these two as options of their own, the others in a list mpv splits on commas:
    // a value with one there would become bogus headers (every Chrome user agent has one)
    val named = headers.mapKeys { (name, _) ->
        when {
            name.equals("User-Agent", ignoreCase = true) -> "User-Agent"
            name.equals("Referer", ignoreCase = true) -> "Referer"
            else -> name
        }
    }
    return UriMediaData(
        url,
        named.filter { (name, value) -> name == "User-Agent" || name == "Referer" || ',' !in value },
        MediaExtraFiles(subtitles),
    )
}

/**
 * mpv, as `rememberMediampPlayer` gives it, its streams resolved with the app's DNS: the system's may
 * block the hosts. After a stall it waits for as much video as the Android player does: resumed with
 * 1 s, a host slower than its stream (AnsEmbed's at times) stutters every few seconds.
 */
@Composable
private fun rememberMpvPlayer(): MediampPlayer {
    val player = remember {
        MpvMediampPlayer(Unit, EmptyCoroutineContext, configureOptions = {
            it.option("http-proxy", DnsProxy.url)
            it.option("cache-pause-wait", "3.5")
        })
    }
    DisposableEffect(player) {
        onDispose { player.close() }
    }
    return player
}

/** What the window shows while the TV plays the episode, under the controls that drive it. */
@Composable
private fun CastingPanel(device: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.CastConnected, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
        Text(
            text = stringResource(Res.string.cast_devices_casting, device),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

private const val CONTROLS_HIDE_DELAY = 3000L

private val HiddenCursor = PointerIcon(
    Toolkit.getDefaultToolkit().createCustomCursor(BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), Point(0, 0), "hidden"),
)
