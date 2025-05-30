package com.sf.tadami.ui.animeinfos.episode.player

import android.annotation.SuppressLint
import android.net.Uri
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.OptIn
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.sf.tadami.R
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.network.player.PlayerNetworkHelper
import com.sf.tadami.preferences.advanced.AdvancedPreferences
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.source.model.Track
import com.sf.tadami.ui.animeinfos.episode.EpisodeActivity
import com.sf.tadami.ui.animeinfos.episode.player.controls.PlayerControls
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.EpisodesDialog
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.settings.SettingsDialog
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.settings.tabs.toSubtitlesStyle
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.tracksselection.TracksSelectionDialog
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.videoselection.VideoSelectionDialog
import com.sf.tadami.ui.animeinfos.episode.player.subtitles.CustomSubtitleParserFactory
import com.sf.tadami.ui.components.widgets.ContentLoader
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.utils.convertToIetfLanguageTag
import kotlinx.coroutines.delay
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.seconds

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    modifier: Modifier = Modifier,
    playerNetworkHelper: PlayerNetworkHelper = Injekt.get(),
    dispatcher: OnBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher,
    @SuppressLint("ContextCastToActivity") playerViewModel: PlayerViewModel = viewModel(LocalContext.current as EpisodeActivity),
    setPlayer: (ExoPlayer) -> Unit,
    onWebViewOpen: () -> Unit,
    setPipMode: () -> Unit,
    refreshPipUi: () -> Unit
) {
    val context = LocalContext.current

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

    val upstreamDataSource = DefaultHttpDataSource.Factory()

    val cacheDataSourceFactory = remember {
        CacheDataSource.Factory()
            .setCache(playerNetworkHelper.cache)
            .setUpstreamDataSourceFactory(upstreamDataSource)
    }

    val resolvingDataSource = ResolvingDataSource.Factory(
        cacheDataSourceFactory
    ) { dataSpec ->
        if (episodeUiState.selectedSource?.headers == null) {
            dataSpec.withRequestHeaders(
                mapOf(
                    Pair(
                        "User-Agent",
                        advancedPreferences.userAgent
                    )
                )
            )
        } else {
            dataSpec.withRequestHeaders(playerViewModel.getSourceHeaders())
        }
    }

    val dataSourceFactory = remember {
        DefaultMediaSourceFactory(context).apply {
            setDataSourceFactory(resolvingDataSource)
            setSubtitleParserFactory(CustomSubtitleParserFactory())
        }
    }

    val loadControl = remember {
        DefaultLoadControl.Builder().apply {
            setBufferDurationsMs(300000, 300000, 1500, 3500)
            setBackBuffer(300000, false)
            setPrioritizeTimeOverSizeThresholds(true)
        }.build()
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .apply {
                setSeekBackIncrementMs(playerPreferences.doubleTapLength)
                setSeekForwardIncrementMs(playerPreferences.doubleTapLength)
                setMediaSourceFactory(dataSourceFactory)
                setLoadControl(loadControl)

            }
            .build()
    }

    var playerView by remember { mutableStateOf<PlayerView?>(null) }

    LaunchedEffect(exoPlayer) {
        setPlayer(exoPlayer)
    }

    LaunchedEffect(exoPlayer.isPlaying) {
        refreshPipUi()
    }

    val episodeNumber by remember(currentEpisode) { derivedStateOf { currentEpisode?.episodeNumber } }

    var shouldShowControls by remember { mutableStateOf(true) }

    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }

    var totalDuration by remember { mutableLongStateOf(0L) }

    var currentTime by remember { mutableLongStateOf(0L) }

    var bufferedPercentage by remember { mutableIntStateOf(0) }

    var playbackState by remember { mutableIntStateOf(exoPlayer.playbackState) }

    var openVideoSelectionDialog by remember { mutableStateOf(false) }

    var openTracksSelectionDialog by remember { mutableStateOf(false) }

    var openSettingsDialog by remember { mutableStateOf(false) }

    var openEpisodesDialog by remember { mutableStateOf(false) }

    fun updateTime() {
        (context as EpisodeActivity).setUpdateTimeJob(
            playerViewModel.updateTime(
                currentEpisode,
                totalDuration,
                currentTime,
                playerPreferences.seenThreshold
            )
        )
    }

    fun selectEpisode(episode: Episode) {
        updateTime()
        playerViewModel.setCurrentEpisode(episode)
    }

    LaunchedEffect(key1 = episodeUiState.loadError) {
        if (episodeUiState.loadError) {
            exoPlayer.release()
            dispatcher.onBackPressed()
        }
    }
    LaunchedEffect(
        playerPreferences.subtitlesEnabled,
        playerPreferences.subtitlePrefLanguages,
        episodeUiState.selectedSubtitleTrack,
        episodeUiState.selectedSource
    ) {
        val preferredLanguages = playerPreferences.subtitlePrefLanguages.split(",")
        exoPlayer.trackSelectionParameters = buildTrackParameters(
            player = exoPlayer,
            subtitlesEnabled = playerPreferences.subtitlesEnabled,
            preferredLanguages = preferredLanguages,
            selectedTrack = episodeUiState.selectedSubtitleTrack,
            selectedSource = episodeUiState.selectedSource
        )
    }

    LaunchedEffect(key1 = episodeUiState.selectedSource) {
        episodeUiState.selectedSource?.let {
            playerViewModel.getDbEpisodeTime { timeSeen ->
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true

                val item = MediaItem.Builder().apply {
                    setUri(it.url)
                    if (it.subtitleTracks.isNotEmpty()) {
                        val subtitlesConfigurations = it.subtitleTracks.filter {
                            it.mimeType !== MimeTypes.TEXT_UNKNOWN
                        }.mapIndexed { index,sub ->
                            MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                                .setMimeType(sub.mimeType)
                                .setLanguage(sub.lang.convertToIetfLanguageTag() + index)
                                .build()
                        }
                        setSubtitleConfigurations(subtitlesConfigurations)
                    }
                }.build()

                exoPlayer.setMediaItem(
                    item,
                    timeSeen.takeIf { it > 0 } ?: currentTime)
                refreshPipUi()
            }
        }
    }

    if (playerPreferences.autoPlay) {
        LaunchedEffect(exoPlayer.isPlaying.not() && playbackState == STATE_ENDED) {
            val autoIdle = exoPlayer.isPlaying.not() && playbackState == STATE_ENDED
            if (currentTime > 0L && totalDuration > 0L) {
                if (autoIdle && hasNextIterator.hasPrevious()) {
                    val next = hasNextIterator.previous()
                    exoPlayer.clearMediaItems()
                    selectEpisode(next)
                }
            }
        }
    }

    if (isPlaying) {
        LaunchedEffect(Unit) {
            while (true) {
                currentTime =
                    exoPlayer.currentPosition.coerceAtLeast(0L).coerceAtMost(totalDuration)
                delay(1.seconds / 30)
            }
        }
    }

    LaunchedEffect(playerPreferences) {
        playerView?.subtitleView?.apply {
            setApplyEmbeddedStyles(false)
            setStyle(playerPreferences.toSubtitlesStyle(context))
            setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, playerPreferences.subtitleTextSize.toFloat())
        }
    }

    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    ContentLoader(isLoading = playerScreenLoading, delay = 500) {
        Box(modifier = modifier) {

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
                        if (!playerInitiatedPause) {
                            exoPlayer.play()
                        }
                    }
                )
                TracksSelectionDialog(
                    opened = openTracksSelectionDialog,
                    subtitleTracks = episodeUiState.selectedSource?.subtitleTracks,
                    selectedSubtitleTrack = episodeUiState.selectedSubtitleTrack,
                    onSubtitleTrackSelected = {
                        playerViewModel.selectedSubtitleTrack(it)
                    },
                    onDismissRequest = {
                        openTracksSelectionDialog = false
                        if (!playerInitiatedPause) {
                            exoPlayer.play()
                        }
                    }
                )
            }

            EpisodesDialog(
                opened = openEpisodesDialog,
                onDismissRequest = {
                    openEpisodesDialog = false
                    if (!playerInitiatedPause) {
                        exoPlayer.play()
                    }
                },
                onConfirm = {
                    exoPlayer.clearMediaItems()
                    selectEpisode(it)
                },
                displayMode = anime?.displayMode,
                episodes = episodes,
                initialEpisode = currentEpisode
            )

            SettingsDialog(
                opened = openSettingsDialog,
                onDismissRequest = {
                    openSettingsDialog = false
                    if (!playerInitiatedPause) {
                        exoPlayer.play()
                    }
                },
                sourceDatastore = playerViewModel.sourceDataStore,
                sourcePrefsitems = playerViewModel.sourceDataStoreScreen
            )

            AndroidView(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                shouldShowControls = shouldShowControls.not()
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { _, _ -> }
                    },
                factory = {
                    PlayerView(it).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        keepScreenOn = true

                        playerView = this

                        subtitleView?.apply {
                            setApplyEmbeddedStyles(false)
                            setStyle(playerPreferences.toSubtitlesStyle(it))
                            setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, playerPreferences.subtitleTextSize.toFloat())
                        }
                    }
                }
            )



            DisposableEffect(
                Unit
            ) {
                val listener = object : Player.Listener {

                    override fun onRenderedFirstFrame() {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                            .build()
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        if (episodeUiState.selectedSubtitleTrack == null && playerPreferences.subtitlesEnabled) {
                            tracks.groups
                                .filter { it.type == C.TRACK_TYPE_TEXT }
                                .forEachIndexed { index, trackGroup ->
                                    if (trackGroup.isSelected && trackGroup.isTrackSelected(0)) {
                                        episodeUiState.selectedSource?.subtitleTracks?.getOrNull(
                                            (index - 1).coerceAtLeast(0)
                                        )?.let { track ->
                                            playerViewModel.selectedSubtitleTrack(track)
                                        }
                                    }
                                }
                        }
                    }

                    override fun onEvents(
                        player: Player, events: Player.Events
                    ) {
                        if (events.contains(Player.EVENT_PLAYER_ERROR)) {
                            player.clearMediaItems()
                            player.prepare()
                            UiToasts.showToast(
                                stringRes = R.string.player_screen_source_load_error,
                                args = arrayOf("${episodeUiState.selectedSource?.fullName}")
                            )
                        } else {
                            super.onEvents(player, events)
                            totalDuration = player.duration.coerceAtLeast(0L)
                            currentTime =
                                player.currentPosition.coerceAtLeast(0L).coerceAtMost(totalDuration)
                            bufferedPercentage = player.bufferedPercentage
                            isPlaying = player.isPlaying
                            playbackState = player.playbackState
                        }
                    }
                }
                exoPlayer.addListener(listener)

                val observer = LifecycleEventObserver { _, event ->

                    when (event) {
                        Lifecycle.Event.ON_PAUSE -> {
                            updateTime()
                            if (!lockedControls) {
                                exoPlayer.pause()
                            }
                        }

                        Lifecycle.Event.ON_RESUME -> {
                            if (!playerInitiatedPause) {
                                exoPlayer.play()
                            }
                        }

                        else -> {}
                    }
                }
                val lifecycle = lifecycleOwner.value.lifecycle
                lifecycle.addObserver(observer)

                exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT

                onDispose {
                    updateTime()
                    lifecycle.removeObserver(observer)
                    exoPlayer.removeListener(listener)
                    exoPlayer.release()
                    playerView = null
                }
            }

            ContentLoader(isLoading = isFetchingSources) {}

            val isVideoLoading by remember(exoPlayer.playbackState, exoPlayer.playWhenReady) {
                derivedStateOf {
                    exoPlayer.playbackState == Player.STATE_BUFFERING
                            && exoPlayer.playWhenReady
                }
            }

            if (isVideoLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .wrapContentSize(), strokeWidth = 3.dp
                )
            }

            PlayerControls(
                modifier = Modifier.fillMaxSize(),
                isVisible = { shouldShowControls },
                isPlaying = isPlaying,
                title = { anime?.title ?: "" },
                episode = when (anime?.displayMode) {
                    is Anime.DisplayMode.NAME -> currentEpisode?.name
                        ?: "${stringResource(id = R.string.player_screen_episode_label)} $episodeNumber"

                    else -> "${stringResource(id = R.string.player_screen_episode_label)} $episodeNumber"
                },
                onReplay = { exoPlayer.seekTo(currentTime - playerPreferences.doubleTapLength) },
                onSkipOp = { exoPlayer.seekTo(currentTime + 85000) },
                onForward = { exoPlayer.seekTo(currentTime + playerPreferences.doubleTapLength) },
                isIdle = exoPlayer.isPlaying.not() && playbackState == STATE_ENDED,
                onTapYoutube = {
                    shouldShowControls = shouldShowControls.not()
                },
                playerSeekValue = playerPreferences.doubleTapLength,
                onPauseToggle = {
                    when {
                        exoPlayer.isPlaying -> {
                            playerViewModel.setPlayerInitadtedPause(true)
                            exoPlayer.pause()
                        }

                        exoPlayer.isPlaying.not() && playbackState == STATE_ENDED -> {
                            exoPlayer.seekTo(0)
                            exoPlayer.playWhenReady = true
                        }

                        else -> {
                            playerViewModel.setPlayerInitadtedPause(false)
                            exoPlayer.play()
                        }
                    }
                    isPlaying = isPlaying.not()
                },
                onStreamSettings = {
                    exoPlayer.pause()
                    openVideoSelectionDialog = openVideoSelectionDialog.not()
                },
                onTracksSettings = {
                    exoPlayer.pause()
                    openTracksSelectionDialog = openTracksSelectionDialog.not()
                },
                totalDuration = { totalDuration },
                currentTime = { currentTime },
                bufferedPercentage = { bufferedPercentage },
                onSeekChanged = { timeMs: Float ->
                    exoPlayer.seekTo(timeMs.toLong())
                },
                onBack = {
                    exoPlayer.release()
                    dispatcher.onBackPressed()
                },
                onNext = {
                    val next = hasNextIterator.previous()
                    exoPlayer.clearMediaItems()
                    selectEpisode(next)
                },
                onPrevious = {
                    val previous = hasPreviousIterator.next()
                    exoPlayer.clearMediaItems()
                    selectEpisode(previous)
                },
                hasNext = {
                    hasNextIterator.hasPrevious()
                },
                hasPrevious = {
                    hasPreviousIterator.hasNext()
                },
                videoSettingsEnabled = episodeUiState.availableSources.isNotEmpty(),
                tracksSettingsEnabled = episodeUiState.selectedSource?.subtitleTracks?.isNotEmpty() ?: false,
                onEpisodesClicked = {
                    exoPlayer.pause()
                    openEpisodesDialog = true
                },
                onPlayerSettings = {
                    exoPlayer.pause()
                    openSettingsDialog = true
                },
                onPipClicked = setPipMode,
                lockedControls = lockedControls,
                onWebViewOpen = onWebViewOpen
            )

        }
    }
}

private fun buildTrackParameters(
    player: ExoPlayer,
    subtitlesEnabled: Boolean,
    preferredLanguages: List<String>,
    selectedTrack: Track.SubtitleTrack?,
    selectedSource: StreamSource?
): TrackSelectionParameters {
    return player.trackSelectionParameters
        .buildUpon()
        .apply {
            // First, handle the basic subtitle preferences
            setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subtitlesEnabled)
            if (subtitlesEnabled) {
                if (selectedTrack != null && selectedSource != null) {
                    // If we have a specific track selected, use that
                    clearOverrides()
                    val trackIndex = selectedSource.subtitleTracks.indexOf(selectedTrack)
                    setPreferredTextLanguage(selectedTrack.lang.convertToIetfLanguageTag() + trackIndex)
                } else {
                    // Otherwise, use the preferred languages
                    setPreferredTextLanguages(*preferredLanguages.toTypedArray())
                }
            }
        }
        .build()
}



