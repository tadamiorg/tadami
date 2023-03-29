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
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.network.api.online.AnimeSourceBase
import com.sf.tadami.ui.animeinfos.episode.PlayerViewModel
import com.sf.tadami.ui.animeinfos.episode.player.controls.PlayerControls
import com.sf.tadami.ui.animeinfos.episode.player.controls.QualityDialog
import com.sf.tadami.ui.components.widgets.ContentLoader
import com.sf.tadami.ui.utils.UiToasts
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import com.sf.tadami.R
@Composable
fun VideoPlayer(
    modifier: Modifier = Modifier,
    dispatcher: OnBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher,
    playerViewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current

    val playerScreenLoading by playerViewModel.playerScreenLoading.collectAsState()

    val episodeUiState by playerViewModel.uiState.collectAsState()
    val currentEpisode by playerViewModel.currentEpisode.collectAsState()

    val isFetchingSources by playerViewModel.isFetchingSources.collectAsState()

    val animeTitle by playerViewModel.animeTitle.collectAsState()

    val episodesList by playerViewModel.episodes.collectAsState()

    val upstreamDataSource = DefaultHttpDataSource.Factory()

    val resolvingDataSource = ResolvingDataSource.Factory(
        upstreamDataSource
    ) { dataSpec ->
        if (episodeUiState.selectedSource?.headers == null) {
            dataSpec.withAdditionalHeaders(
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
                setSeekBackIncrementMs(10000)
                setSeekForwardIncrementMs(10000)
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

    fun selectEpisode(episode: Episode) {
        playerViewModel.updateTime(currentEpisode,totalDuration, currentTime)
        playerViewModel.setCurrentEpisode(episode)
    }

    LaunchedEffect(key1 = episodeUiState.selectedSource) {
        episodeUiState.selectedSource?.let {
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            val item = MediaItem.Builder().apply {
                setUri(
                    it.url
                )
            }.build()

            exoPlayer.setMediaItem(item, currentEpisode?.timeSeen.takeIf { it!! > 0 } ?: currentTime)
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

    val hasNextIterator by remember(episodesList, currentEpisode) {
        derivedStateOf {
            episodesList.listIterator(episodesList.indexOf(currentEpisode))
        }
    }

    val hasPreviousIterator by remember(episodesList, currentEpisode) {
        derivedStateOf {
            episodesList.listIterator(episodesList.indexOf(currentEpisode) + 1)
        }
    }

    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    ContentLoader(isLoading = playerScreenLoading, delay = 500) {
        Box(modifier = modifier) {

            if(episodeUiState.availableSources.isNotEmpty()){
                QualityDialog(
                    opened = openDialog,
                    sources = episodeUiState.availableSources,
                    onSelectSource = { playerViewModel.selectSource(it) },
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

                            fitsSystemWindows = true
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
                                R.string.player_screen_source_load_error,
                                "${episodeUiState.selectedSource?.quality}"
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
                            playerViewModel.updateTime(currentEpisode,totalDuration, currentTime)
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
                    playerViewModel.updateTime(currentEpisode,totalDuration, currentTime)
                    lifecycle.removeObserver(observer)
                    exoPlayer.removeListener(listener)
                    exoPlayer.release()
                }
            }

            ContentLoader(isLoading = isFetchingSources) {}

            PlayerControls(
                modifier = Modifier.fillMaxSize(),
                isVisible = { shouldShowControls },
                isPlaying = { isPlaying },
                title = { animeTitle },
                episode = "${stringResource(id = R.string.player_screen_episode_label)} $episodeNumber",
                playbackState = { playbackState },
                onReplay = { exoPlayer.seekBack() },
                onSkipOp = { exoPlayer.seekTo(currentTime + 85000) },
                onForward = { exoPlayer.seekForward() },
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
                onCast = {

                },
                onNext = {
                    if (hasNextIterator.hasPrevious()) {
                        val next = hasNextIterator.previous()
                        exoPlayer.clearMediaItems()
                        selectEpisode(next)
                    }
                },
                onPrevious = {
                    if (hasPreviousIterator.hasNext()) {
                        val previous = hasPreviousIterator.next()
                        exoPlayer.clearMediaItems()
                        selectEpisode(previous)
                    }
                },
                videoSettingsEnabled = episodeUiState.availableSources.isNotEmpty()
            )
        }
    }
}

