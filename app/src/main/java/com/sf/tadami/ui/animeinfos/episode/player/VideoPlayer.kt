package com.sf.tadami.ui.animeinfos.episode.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.OptIn
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.sf.tadami.R
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.preferences.advanced.AdvancedPreferences
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.ui.animeinfos.episode.EpisodeActivity
import com.sf.tadami.ui.animeinfos.episode.PlayerViewModel
import com.sf.tadami.ui.animeinfos.episode.player.controls.PlayerControls
import com.sf.tadami.ui.animeinfos.episode.player.controls.QualityDialog
import com.sf.tadami.ui.components.widgets.ContentLoader
import com.sf.tadami.ui.utils.UiToasts
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@OptIn(UnstableApi::class) @Composable
fun VideoPlayer(
    modifier: Modifier = Modifier,
    dispatcher: OnBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher,
    playerViewModel: PlayerViewModel = viewModel(LocalContext.current as EpisodeActivity)
) {
    val context = LocalContext.current

    val playerPreferences by rememberDataStoreState(customPrefs = PlayerPreferences).value.collectAsState()
    val advancedPreferences by rememberDataStoreState(customPrefs = AdvancedPreferences).value.collectAsState()

    val playerScreenLoading by playerViewModel.playerScreenLoading.collectAsState()

    val episodeUiState by playerViewModel.uiState.collectAsState()
    val currentEpisode by playerViewModel.currentEpisode.collectAsState()

    val isFetchingSources by playerViewModel.isFetchingSources.collectAsState()

    val anime by playerViewModel.anime.collectAsState()

    val upstreamDataSource = DefaultHttpDataSource.Factory()

    val hasNextIterator by playerViewModel.hasNextIterator.collectAsState()
    val hasPreviousIterator by playerViewModel.hasPreviousIterator.collectAsState()

    val resolvingDataSource = ResolvingDataSource.Factory(
        upstreamDataSource
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
            dataSpec.withRequestHeaders(episodeUiState.selectedSource!!.headers!!.toMap())
        }
    }

    val dataSourceFactory = remember {
        DefaultMediaSourceFactory(context).apply {
            setDataSourceFactory(resolvingDataSource)
        }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .apply {
                setSeekBackIncrementMs(playerPreferences.doubleTapLength)
                setSeekForwardIncrementMs(playerPreferences.doubleTapLength)
                setMediaSourceFactory(dataSourceFactory)
            }
            .build()
    }


    val episodeNumber by remember(currentEpisode) { derivedStateOf { currentEpisode?.episodeNumber } }

    var shouldShowControls by remember { mutableStateOf(true) }

    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }

    var totalDuration by remember { mutableStateOf(0L) }

    var currentTime by remember { mutableStateOf(0L) }

    var bufferedPercentage by remember { mutableStateOf(0) }

    var playbackState by remember { mutableStateOf(exoPlayer.playbackState) }

    var openDialog by remember { mutableStateOf(false) }

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

    LaunchedEffect(key1 = episodeUiState.selectedSource) {
        episodeUiState.selectedSource?.let {
            playerViewModel.getDbEpisodeTime { timeSeen ->
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
                val item = MediaItem.Builder().apply {
                    setUri(
                        it.url
                    )
                }.build()

                exoPlayer.setMediaItem(
                    item,
                    timeSeen.takeIf { it > 0 } ?: currentTime)
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

    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    ContentLoader(isLoading = playerScreenLoading, delay = 500) {
        Box(modifier = modifier) {

            if (episodeUiState.availableSources.isNotEmpty()) {
                QualityDialog(
                    opened = openDialog,
                    sources = episodeUiState.availableSources,
                    onSelectSource = {
                        updateTime()
                        playerViewModel.selectSource(it)
                    },
                    selectedSource = episodeUiState.selectedSource,
                    onDismissRequest = { openDialog = false }
                )
            }


            DisposableEffect(
                AndroidView(
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    with(size.width) {
                                        when {
                                            it.x < this * 0.45 -> {
                                                exoPlayer.seekBack()
                                            }

                                            it.x > this * 0.55 -> {
                                                exoPlayer.seekForward()
                                            }
                                        }
                                    }
                                },
                                onTap = {
                                    shouldShowControls = shouldShowControls.not()
                                }
                            )
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
                            keepScreenOn = true
                        }
                    }
                )
            ) {
                val listener = object : Player.Listener {
                    override fun onEvents(
                        player: Player, events: Player.Events
                    ) {
                        if (events.contains(Player.EVENT_PLAYER_ERROR)) {
                            player.clearMediaItems()
                            player.prepare()
                            UiToasts.showToast(
                                stringRes = R.string.player_screen_source_load_error,
                                args = arrayOf("${episodeUiState.selectedSource?.quality}")
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
                            exoPlayer.pause()
                        }

                        Lifecycle.Event.ON_RESUME -> {
                            exoPlayer.play()
                        }

                        else -> {}
                    }
                }
                val lifecycle = lifecycleOwner.value.lifecycle
                lifecycle.addObserver(observer)

                onDispose {
                    updateTime()
                    lifecycle.removeObserver(observer)
                    exoPlayer.removeListener(listener)
                    exoPlayer.release()
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
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).wrapContentSize(),strokeWidth = 3.dp)
            }

            PlayerControls(
                modifier = Modifier.fillMaxSize(),
                isVisible = { shouldShowControls },
                isPlaying = isPlaying,
                title = { anime?.title ?: "" },
                episode = "${stringResource(id = R.string.player_screen_episode_label)} $episodeNumber",
                onReplay = { exoPlayer.seekBack() },
                onSkipOp = { exoPlayer.seekTo(currentTime + 85000) },
                onForward = { exoPlayer.seekForward() },
                isIdle = exoPlayer.isPlaying.not() && playbackState == STATE_ENDED,
                onPauseToggle = {
                    when {
                        exoPlayer.isPlaying -> {
                            exoPlayer.pause()
                        }

                        exoPlayer.isPlaying.not() && playbackState == STATE_ENDED -> {
                            exoPlayer.seekTo(0)
                            exoPlayer.playWhenReady = true
                        }

                        else -> {
                            exoPlayer.play()
                        }
                    }
                    isPlaying = isPlaying.not()
                },
                onSettings = { openDialog = openDialog.not() },
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
                onCast = {},
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
                videoSettingsEnabled = episodeUiState.availableSources.isNotEmpty()
            )
        }
    }
}

