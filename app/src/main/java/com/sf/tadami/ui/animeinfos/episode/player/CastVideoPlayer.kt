package com.sf.tadami.ui.animeinfos.episode.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.util.Log
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.android.gms.cast.MediaError
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.media.RemoteMediaClient.Callback
import com.google.android.gms.cast.framework.media.RemoteMediaClient.ProgressListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient.RESUME_STATE_PLAY
import com.sf.tadami.R
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.ui.animeinfos.episode.EpisodeActivity
import com.sf.tadami.ui.animeinfos.episode.cast.channels.CastErrorCode
import com.sf.tadami.ui.animeinfos.episode.cast.channels.ErrorChannel
import com.sf.tadami.ui.animeinfos.episode.cast.channels.TadamiCastError
import com.sf.tadami.ui.animeinfos.episode.cast.channels.tadamiCastMessageCallback
import com.sf.tadami.ui.animeinfos.episode.cast.isCastMediaFinished
import com.sf.tadami.ui.animeinfos.episode.player.controls.PlayerControls
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.EpisodesDialog
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.QualityDialog
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.settings.SettingsDialog
import com.sf.tadami.ui.components.widgets.ContentLoader
import com.sf.tadami.ui.utils.ImageDefaults
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("SourceLockedOrientationActivity")
@Composable
fun CastVideoPlayer(
    modifier: Modifier = Modifier,
    dispatcher: OnBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher,
    playerViewModel: PlayerViewModel = viewModel(LocalContext.current as EpisodeActivity),
    snackbarHostState: SnackbarHostState,
    onWebViewOpen : () -> Unit,
    castSession: CastSession
) {

    val activityContext = LocalContext.current as EpisodeActivity

    val playerPreferences by rememberDataStoreState(customPrefs = PlayerPreferences).value.collectAsState()

    val playerScreenLoading by playerViewModel.playerScreenLoading.collectAsState()

    val episodeUiState by playerViewModel.uiState.collectAsState()
    val currentEpisode by playerViewModel.currentEpisode.collectAsState()
    val episodes by playerViewModel.episodes.collectAsState()

    val isFetchingSources by playerViewModel.isFetchingSources.collectAsState()

    val anime by playerViewModel.anime.collectAsState()

    val hasNextIterator by playerViewModel.hasNextIterator.collectAsState()
    val hasPreviousIterator by playerViewModel.hasPreviousIterator.collectAsState()

    val episodeNumber by remember(currentEpisode) { derivedStateOf { currentEpisode?.episodeNumber } }

    var totalDuration by remember { mutableStateOf(0L) }

    var currentTime by remember { mutableStateOf(0L) }

    var isPlaying by remember { mutableStateOf(castSession.remoteMediaClient?.isPlaying ?: false) }

    var debounceSeekJob: Job? by remember { mutableStateOf(null) }
    val coroutineScope = rememberCoroutineScope()

    val idleLock by playerViewModel.idleLock.collectAsState()

    var isIdle by remember { mutableStateOf(isCastMediaFinished(castSession.remoteMediaClient?.idleReason)) }

    var openStreamDialog by remember { mutableStateOf(false) }

    var openSettingsDialog by remember { mutableStateOf(false) }

    var openEpisodesDialog by remember { mutableStateOf(false) }

    fun updateTime() {
        activityContext.setUpdateTimeJob(
            playerViewModel.updateTime(
                currentEpisode,
                totalDuration,
                currentTime,
                playerPreferences.seenThreshold
            )
        )
    }

    fun resetTimers() {
        activityContext.stopCastEpisode()?.setResultCallback {
            currentTime = 0
            totalDuration = 0
        }
    }

    fun selectEpisode(episode: Episode) {
        updateTime()
        resetTimers()
        playerViewModel.setCurrentEpisode(episode)
    }

    DisposableEffect(Unit) {
        activityContext.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        val mediaCallback = object : Callback() {
            override fun onStatusUpdated() {
                super.onStatusUpdated()
                isPlaying = castSession.remoteMediaClient!!.isPlaying
                isIdle =
                    isCastMediaFinished(castSession.remoteMediaClient!!.idleReason) && castSession.remoteMediaClient!!.loadingItem == null
            }

            override fun onMediaError(error: MediaError) {
                super.onMediaError(error)
                when (error.reason) {
                    MediaError.ERROR_REASON_APP_ERROR -> {
                        Log.e("Media Error", error.toJson().toString())
                    }

                    else -> {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                getErrorMessage(
                                    activityContext,
                                    error.detailedErrorCode
                                )
                            )
                        }
                    }
                }
            }
        }

        val progressListener = ProgressListener { progress, duration ->
            if (castSession.isConnected && !castSession.isConnecting && isPlaying) {
                currentTime = progress.coerceAtLeast(0)
                totalDuration = duration.coerceAtLeast(0)
            }
        }

        val messageReceiverCallback = tadamiCastMessageCallback { _, _, message: TadamiCastError ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(getErrorMessage(activityContext, message.errorCode))
            }
        }

        castSession.setMessageReceivedCallbacks(ErrorChannel.NAMESPACE, messageReceiverCallback)
        castSession.remoteMediaClient?.registerCallback(mediaCallback)
        castSession.remoteMediaClient?.addProgressListener(progressListener, 1000L)
        onDispose {
            updateTime()
            castSession.removeMessageReceivedCallbacks(ErrorChannel.NAMESPACE)
            castSession.remoteMediaClient?.removeProgressListener(progressListener)
            castSession.remoteMediaClient?.unregisterCallback(mediaCallback)
            activityContext.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        }
    }

    LaunchedEffect(key1 = episodeUiState.loadError) {
        if (episodeUiState.loadError) {
            dispatcher.onBackPressed()
        }
    }

    LaunchedEffect(idleLock) {
        if (idleLock) {
            totalDuration = 0
            currentTime = 0
        }
    }

    if (playerPreferences.autoPlay) {
        LaunchedEffect(isIdle && !idleLock) {
            val autoIdle = isIdle && !idleLock
            if (currentTime > 0L && totalDuration > 0L) {
                if (autoIdle && hasNextIterator.hasPrevious()) {
                    val next = hasNextIterator.previous()
                    selectEpisode(next)
                }
            }
        }
    }

    ContentLoader(isLoading = playerScreenLoading, delay = 500) {
        Box(modifier = modifier) {

            if (episodeUiState.availableSources.isNotEmpty()) {
                QualityDialog(
                    opened = openStreamDialog,
                    sources = episodeUiState.availableSources,
                    onSelectSource = {
                        updateTime()
                        playerViewModel.selectSource(it)
                    },
                    selectedSource = episodeUiState.selectedSource,
                    onDismissRequest = {
                        openStreamDialog = false
                    }
                )
            }

            EpisodesDialog(
                opened = openEpisodesDialog,
                onDismissRequest = {
                    openEpisodesDialog = false
                },
                onConfirm = {
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
                },
                sourceDatastore = playerViewModel.sourceDataStore,
                sourcePrefsitems = playerViewModel.sourceDataStoreScreen
            )

            AsyncImage(
                model = anime?.thumbnailUrl,
                placeholder = ColorPainter(ImageDefaults.CoverPlaceholderColor),
                error = painterResource(R.drawable.cover_error),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(2f / 3f)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop,
            )

            ContentLoader(isLoading = isFetchingSources) {}

            PlayerControls(
                modifier = Modifier.fillMaxSize(),
                isVisible = { true },
                isPlaying = isPlaying,
                title = { anime?.title ?: "" },
                episode = "${stringResource(id = R.string.player_screen_episode_label)} $episodeNumber",
                isIdle = isIdle && !idleLock,
                idleLock = idleLock,
                onReplay = {
                    debounceSeekJob?.cancel()
                    if (isPlaying) {
                        isPlaying = false
                        castSession.remoteMediaClient!!.pause()
                    }
                    currentTime =
                        (currentTime - playerPreferences.doubleTapLength).coerceAtLeast(0L)
                    debounceSeekJob = coroutineScope.launch {
                        delay(500.milliseconds)
                        castSession.remoteMediaClient!!.seek(getSeek(currentTime))
                    }
                },
                onSkipOp = {
                    debounceSeekJob?.cancel()
                    if (isPlaying) {
                        isPlaying = false
                        castSession.remoteMediaClient!!.pause()
                    }
                    currentTime = (currentTime + 85000).coerceAtMost(totalDuration)
                    debounceSeekJob = coroutineScope.launch {
                        delay(500.milliseconds)
                        castSession.remoteMediaClient!!.seek(getSeek(currentTime))
                    }
                },
                onForward = {
                    debounceSeekJob?.cancel()
                    if (isPlaying) {
                        isPlaying = false
                        castSession.remoteMediaClient!!.pause()
                    }
                    currentTime =
                        (currentTime + playerPreferences.doubleTapLength).coerceAtMost(totalDuration)
                    debounceSeekJob = coroutineScope.launch {
                        delay(500.milliseconds)
                        castSession.remoteMediaClient!!.seek(getSeek(currentTime))
                    }
                },
                onPauseToggle = {
                    if (isIdle && !idleLock) {
                        updateTime()
                        activityContext.retryLoadRequest()
                    } else {
                        castSession.remoteMediaClient!!.togglePlayback()
                    }
                },
                onStreamSettings = { openStreamDialog = true },
                totalDuration = { totalDuration },
                currentTime = { currentTime },
                bufferedPercentage = { 0 },
                onSeekChanged = { timeMs: Float ->
                    if (isPlaying) {
                        isPlaying = false
                        castSession.remoteMediaClient!!.pause()
                    }
                    currentTime = timeMs.toLong()
                },
                onSeekEnd = {
                    debounceSeekJob?.cancel()
                    debounceSeekJob = coroutineScope.launch {
                        delay(500.milliseconds)
                        castSession.remoteMediaClient!!.seek(getSeek(currentTime))
                    }
                },
                onBack = {
                    dispatcher.onBackPressed()
                },
                onNext = {
                    val next = hasNextIterator.previous()
                    selectEpisode(next)
                },
                onPrevious = {
                    val previous = hasPreviousIterator.next()
                    selectEpisode(previous)
                },
                hasNext = {
                    hasNextIterator.hasPrevious()
                },
                hasPrevious = {
                    hasPreviousIterator.hasNext()
                },
                videoSettingsEnabled = episodeUiState.availableSources.isNotEmpty(),
                playerSeekValue = playerPreferences.doubleTapLength,
                onTapYoutube = {},
                onPlayerSettings = {
                    openSettingsDialog = true
                },
                onEpisodesClicked = {
                    openEpisodesDialog = true
                },
                lockedControls = false,
                onWebViewOpen = onWebViewOpen

            )
        }
    }
}

private fun getErrorMessage(context: Context, errorCode: Int?): String {
    return when (errorCode) {
        CastErrorCode.COMMUNICATION.code -> {
            context.resources.getString(R.string.cast_error_communication, errorCode)
        }

        CastErrorCode.UNSUPPORTED.code -> {
            context.getString(R.string.cast_error_unsupported)
        }

        CastErrorCode.LOAD_FAILED.code -> {
            context.getString(R.string.cast_error_load_failed)
        }

        else -> {
            context.resources.getString(R.string.cast_error_unknown, errorCode)
        }
    }
}

private fun getSeek(time: Long): MediaSeekOptions {
    return MediaSeekOptions.Builder().apply {
        setPosition(time)
        setResumeState(RESUME_STATE_PLAY)
    }.build()
}

