package com.sf.tadami.ui.animeinfos.episode

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.exoplayer.ExoPlayer
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.ui.animeinfos.episode.player.ACTION_MEDIA_CONTROL
import com.sf.tadami.ui.animeinfos.episode.player.EXTRA_CONTROL_TYPE
import com.sf.tadami.ui.animeinfos.episode.player.PIP_NEXT
import com.sf.tadami.ui.animeinfos.episode.player.PIP_PAUSE
import com.sf.tadami.ui.animeinfos.episode.player.PIP_PLAY
import com.sf.tadami.ui.animeinfos.episode.player.PIP_PREVIOUS
import com.sf.tadami.ui.animeinfos.episode.player.PIP_SKIP
import com.sf.tadami.ui.animeinfos.episode.player.PictureInPictureHandler
import com.sf.tadami.ui.animeinfos.episode.player.PipState
import com.sf.tadami.ui.animeinfos.episode.player.PlayerViewModel
import com.sf.tadami.ui.animeinfos.episode.player.PlayerViewModelFactory
import com.sf.tadami.ui.animeinfos.episode.player.VideoPlayer
import com.sf.tadami.ui.themes.TadamiTheme
import com.sf.tadami.utils.powerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class EpisodeActivity : AppCompatActivity() {

    private var availableSources: List<StreamSource>? = null
    private var selectedSource: StreamSource? = null
    private var episodeUrl: String? = null
    private var anime: Anime? = null
    private var currentEpisode: Episode? = null
    private var updateTimeJob: Job? = null
    private var pipReceiver: BroadcastReceiver? = null
    private var exoPlayer: ExoPlayer? = null

    private lateinit var playerViewModel: PlayerViewModel

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        super.onCreate(savedInstanceState)


        val viewModel: PlayerViewModel by viewModels(factoryProducer = {
            PlayerViewModelFactory()
        })
        playerViewModel = viewModel


        observeData()

        setContent {
            TadamiTheme(
                isDark = true,
                amoled = true
            ) {
                val snackbarHostState = remember { SnackbarHostState() }
                Scaffold(
                    snackbarHost = {
                        SnackbarHost(
                            modifier = Modifier.padding(bottom = 100.dp),
                            hostState = snackbarHostState
                        )
                    }
                ) {

                    VideoPlayer(
                        setPlayer = {
                            exoPlayer = it
                        },
                        refreshPipUi = {
                            if (PipState.mode == PipState.ON) {
                                updatePip(false)
                            }
                        },
                        setPipMode = {
                            updatePip(true)
                        }
                    )

                }

            }
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    playerViewModel.uiState.collectLatest { uiState ->
                        availableSources = uiState.availableSources
                        selectedSource = uiState.selectedSource
                        episodeUrl = uiState.rawUrl
                    }
                }
                launch {
                    playerViewModel.anime.collectLatest { anime ->
                        this@EpisodeActivity.anime = anime
                    }
                }
                launch {
                    playerViewModel.currentEpisode.collectLatest { episode ->
                        currentEpisode = episode
                    }
                }
            }
        }
    }

    fun setUpdateTimeJob(job: Job?) {
        this.updateTimeJob?.cancel()
        this.updateTimeJob = job
    }

    private fun checkUpdateTimeJobEnded(callback: () -> Unit) {
        if (this.updateTimeJob == null) return callback()
        this.updateTimeJob!!.invokeOnCompletion { cancellation ->
            if (cancellation == null) {
                callback()
                this@EpisodeActivity.updateTimeJob = null
            } else {
                Log.e("Error while updating time : ", cancellation.message.toString())
            }
        }
    }

    /* PIP MODE HANDLING */

    override fun onStop() {
        if (PipState.mode == PipState.ON && powerManager.isInteractive) {
            finishAndRemoveTask()
        }
        super.onStop()
    }

    override fun onDestroy() {
        exoPlayer = null
        super.onDestroy()
    }

    private fun updatePip(start: Boolean) {
        val anime = playerViewModel.anime.value ?: return
        val episode = playerViewModel.currentEpisode.value ?: return
        val paused = exoPlayer?.isPlaying?.not() ?: return
        PictureInPictureHandler().update(
            context = this,
            title = anime.title,
            subtitle = episode.name,
            paused = paused,
            replaceWithPrevious = false,
            pipOnExit = false,
            hasNext = playerViewModel.hasNextIterator.value.hasPrevious(),
            hasPrevious = playerViewModel.hasPreviousIterator.value.hasNext()
        ).let {
            setPictureInPictureParams(it)
            if (PipState.mode == PipState.OFF && start) {
                PipState.mode = PipState.STARTED
                playerViewModel.lockControls(locked = true)
                enterPictureInPictureMode(it)
            }
        }

    }

    @Deprecated("Deprecated in Java")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        PipState.mode = if (isInPictureInPictureMode) PipState.ON else PipState.OFF

        playerViewModel.lockControls(locked = PipState.mode == PipState.ON)
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)

        if (PipState.mode == PipState.ON) {
            pipReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent == null || ACTION_MEDIA_CONTROL != intent.action) {
                        return
                    }
                    when (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
                        PIP_PLAY -> {
                            exoPlayer?.play()
                        }

                        PIP_PAUSE -> {
                            exoPlayer?.pause()
                        }

                        PIP_PREVIOUS -> {
                            if (playerViewModel.hasPreviousIterator.value.hasNext()) {
                                val previous = playerViewModel.hasPreviousIterator.value.next()
                                exoPlayer?.clearMediaItems()
                                playerViewModel.setCurrentEpisode(previous)
                            }
                        }

                        PIP_NEXT -> {
                            if (playerViewModel.hasNextIterator.value.hasPrevious()) {
                                val next = playerViewModel.hasNextIterator.value.previous()
                                exoPlayer?.clearMediaItems()
                                playerViewModel.setCurrentEpisode(next)
                            }
                        }

                        PIP_SKIP -> {
                            exoPlayer?.seekTo((exoPlayer?.currentPosition ?: 0) + 10000)
                        }
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(
                    pipReceiver,
                    IntentFilter(ACTION_MEDIA_CONTROL),
                    RECEIVER_NOT_EXPORTED
                )
            } else {
                registerReceiver(pipReceiver, IntentFilter(ACTION_MEDIA_CONTROL))
            }
        } else {
            if (exoPlayer?.isPlaying == false) {
                playerViewModel.lockControls(
                    false
                )
            }
            if (pipReceiver != null) {
                unregisterReceiver(pipReceiver)
                pipReceiver = null
            }
        }
    }
}