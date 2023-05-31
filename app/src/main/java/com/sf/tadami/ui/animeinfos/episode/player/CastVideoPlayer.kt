package com.sf.tadami.ui.animeinfos.episode.player

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
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
import com.google.android.gms.cast.framework.media.RemoteMediaClient.*
import com.sf.tadami.R
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.ui.animeinfos.episode.EpisodeActivity
import com.sf.tadami.ui.animeinfos.episode.PlayerViewModel
import com.sf.tadami.ui.animeinfos.episode.player.controls.PlayerControls
import com.sf.tadami.ui.animeinfos.episode.player.controls.QualityDialog
import com.sf.tadami.ui.components.widgets.ContentLoader
import com.sf.tadami.ui.tabs.settings.model.rememberDataStoreState
import com.sf.tadami.ui.tabs.settings.screens.player.PlayerPreferences
import com.sf.tadami.ui.utils.ImageDefaults
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@Composable
fun CastVideoPlayer(
    modifier: Modifier = Modifier,
    dispatcher: OnBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher,
    playerViewModel: PlayerViewModel = viewModel(LocalContext.current as EpisodeActivity),
    castSession: CastSession
) {
    val activityContext = LocalContext.current as EpisodeActivity

    val playerPreferences by rememberDataStoreState(customPrefs = PlayerPreferences).value.collectAsState()

    val playerScreenLoading by playerViewModel.playerScreenLoading.collectAsState()

    val episodeUiState by playerViewModel.uiState.collectAsState()
    val currentEpisode by playerViewModel.currentEpisode.collectAsState()

    val isFetchingSources by playerViewModel.isFetchingSources.collectAsState()

    val anime by playerViewModel.anime.collectAsState()

    val hasNextIterator by playerViewModel.hasNextIterator.collectAsState()
    val hasPreviousIterator by playerViewModel.hasPreviousIterator.collectAsState()

    val episodeNumber by remember(currentEpisode) { derivedStateOf { currentEpisode?.episodeNumber } }

    var totalDuration by remember { mutableStateOf(0L) }

    var currentTime by remember { mutableStateOf(0L) }

    var isPlaying by remember { mutableStateOf(castSession.remoteMediaClient?.isPlaying ?: false) }

    var openDialog by remember { mutableStateOf(false) }

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

    fun selectEpisode(episode: Episode) {
        updateTime()
        playerViewModel.setCurrentEpisode(episode)
    }

    DisposableEffect(Unit){
        val mediaCallback = object : Callback() {
            override fun onStatusUpdated() {
                super.onStatusUpdated()
                isPlaying = castSession.remoteMediaClient!!.isPlaying
            }

            override fun onMediaError(error: MediaError) {
                super.onMediaError(error)
            }
        }

        val progressListener = ProgressListener { progress, duration ->
            if(isPlaying){
                currentTime = progress.coerceAtLeast(0)
                totalDuration = duration.coerceAtLeast(0)
            }
        }

        castSession.remoteMediaClient?.registerCallback(mediaCallback)
        castSession.remoteMediaClient?.addProgressListener(progressListener,(1.seconds/15).toLong(DurationUnit.MILLISECONDS))
        onDispose {
            updateTime()
            castSession.remoteMediaClient?.removeProgressListener(progressListener)
            castSession.remoteMediaClient?.unregisterCallback(mediaCallback)
        }
    }

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

            AsyncImage(
                model = anime?.thumbnailUrl,
                placeholder = ColorPainter(ImageDefaults.CoverPlaceholderColor),
                error = painterResource(R.drawable.cover_error),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop,
            )

            ContentLoader(isLoading = isFetchingSources) {}

            PlayerControls(
                modifier = Modifier.fillMaxSize(),
                isVisible = { true },
                isPlaying = isPlaying ,
                title = { anime?.title ?: "" },
                episode = "${stringResource(id = R.string.player_screen_episode_label)} $episodeNumber",
                onReplay = {
                    castSession.remoteMediaClient!!.seek(getSeek(time = 0))
                },
                onSkipOp = {
                    castSession.remoteMediaClient!!.seek(getSeek(currentTime, 85000))
                },
                onForward = {
                    castSession.remoteMediaClient!!.seek(
                        getSeek(
                            currentTime,
                            playerPreferences.doubleTapLength
                        )
                    )
                },
                onPauseToggle = {
                    when (castSession.remoteMediaClient!!.isPlaying) {
                        true -> {
                            castSession.remoteMediaClient!!.pause()
                        }
                        false -> {
                            castSession.remoteMediaClient!!.play()
                        }
                    }
                },
                onSettings = { openDialog = openDialog.not() },
                totalDuration = { totalDuration },
                currentTime = { currentTime },
                bufferedPercentage = { 0 },
                onSeekChanged = { timeMs: Float ->
                    if(castSession.remoteMediaClient?.isPaused == false){
                        castSession.remoteMediaClient!!.pause()
                    }
                    currentTime = timeMs.toLong()
                },
                onSeekEnd = {
                    castSession.remoteMediaClient!!.seek(getSeek(time = currentTime))
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
                videoSettingsEnabled = episodeUiState.availableSources.isNotEmpty()
            )
        }
    }
}

private fun getSeek(currentTime: Long? = null, time: Long): MediaSeekOptions {
    return MediaSeekOptions.Builder().apply {
        setPosition(
            if (currentTime != null) {
                currentTime + time
            } else {
                time
            }
        )
        setResumeState(RESUME_STATE_PLAY)
    }.build()
}

