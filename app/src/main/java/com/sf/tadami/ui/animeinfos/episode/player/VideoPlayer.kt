package com.sf.tadami.ui.animeinfos.episode.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.STATE_ENDED
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.ResolvingDataSource
import com.sf.tadami.R
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.network.api.online.AnimeSourceBase
import com.sf.tadami.ui.animeinfos.episode.EpisodeActivity
import com.sf.tadami.ui.animeinfos.episode.PlayerViewModel
import com.sf.tadami.ui.animeinfos.episode.player.controls.PlayerControls
import com.sf.tadami.ui.animeinfos.episode.player.controls.QualityDialog
import com.sf.tadami.ui.components.widgets.ContentLoader
import com.sf.tadami.ui.tabs.settings.model.rememberDataStoreState
import com.sf.tadami.ui.tabs.settings.screens.player.PlayerPreferences
import com.sf.tadami.ui.utils.UiToasts
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun VideoPlayer(
    modifier: Modifier = Modifier,
    dispatcher: OnBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher,
    playerViewModel: PlayerViewModel = viewModel(LocalContext.current as EpisodeActivity)
) {
    val context = LocalContext.current

    val playerPreferences by rememberDataStoreState(customPrefs = PlayerPreferences).value.collectAsState()

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
                        AnimeSourceBase.DEFAULT_USER_AGENT
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

    if (isPlaying) {
        LaunchedEffect(Unit) {
            while (true) {
                currentTime = exoPlayer.currentPosition.coerceAtLeast(0L)
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
                        StyledPlayerView(it).apply {
                            player = exoPlayer
                            useController = false
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setShowBuffering(StyledPlayerView.SHOW_BUFFERING_ALWAYS)
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
                            currentTime = player.currentPosition.coerceAtLeast(0L)
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

