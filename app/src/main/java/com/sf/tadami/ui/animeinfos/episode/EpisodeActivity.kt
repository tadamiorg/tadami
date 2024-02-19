package com.sf.tadami.ui.animeinfos.episode

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.*
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.images.WebImage
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.notifications.cast.CastProxyService
import com.sf.tadami.source.model.OkhttpHeadersSerializer
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.ui.animeinfos.episode.cast.channels.ErrorChannel
import com.sf.tadami.ui.animeinfos.episode.cast.getLocalIPAddress
import com.sf.tadami.ui.animeinfos.episode.cast.setCastCustomChannel
import com.sf.tadami.ui.animeinfos.episode.player.ACTION_MEDIA_CONTROL
import com.sf.tadami.ui.animeinfos.episode.player.CastVideoPlayer
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.net.URLEncoder


class EpisodeActivity : AppCompatActivity() {

    private val json: Json = Json
    private var castSession: CastSession? = null
    private val isCasting = mutableStateOf(false)
    private lateinit var castContext: CastContext
    private var castSessionManagerListener: SessionManagerListener<CastSession>? = null
    private var castStateListener: CastStateListener? = null
    private var availableSources: List<StreamSource>? = null
    private var selectedSource: StreamSource? = null
    private var episodeUrl: String? = null
    private var anime: Anime? = null
    private var currentEpisode: Episode? = null
    private var updateTimeJob: Job? = null
    private val errorChannel = ErrorChannel()
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

        castContext = CastContext.getSharedInstance(this)
        castSession = castContext.sessionManager.currentCastSession
        isCasting.value = castSession?.isConnected ?: false
        if (castSession?.remoteMediaClient?.mediaInfo?.customData != null) {
            try {
                val customDataSources =
                    castSession?.remoteMediaClient?.mediaInfo?.customData!!.get("availableSources") as String
                val selectedSource =
                    castSession?.remoteMediaClient?.mediaInfo?.customData!!.get("selectedSource") as String

                val rawUrl =
                    castSession?.remoteMediaClient?.mediaInfo?.customData!!.get("episodeUrl") as String
                val episodeId =
                    castSession?.remoteMediaClient?.mediaInfo?.customData!!.get("episodeId") as Int
                val intentEpisodeId = checkNotNull(intent.extras?.getLong("episode")).toInt()
                val isResumedFromCast = episodeId == intentEpisodeId
                val viewModel: PlayerViewModel by viewModels(factoryProducer = {
                    PlayerViewModelFactory(isResumedFromCast)
                })
                playerViewModel = viewModel
                if (isResumedFromCast) {
                    playerViewModel.setResumeFromCastSession(
                        rawUrl = rawUrl,
                        selectedSource = json.decodeFromString(
                            selectedSource
                        ),
                        availableSources = json.decodeFromString(
                            customDataSources
                        )

                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        } else {
            val viewModel: PlayerViewModel by viewModels(factoryProducer = {
                PlayerViewModelFactory()
            })
            playerViewModel = viewModel
        }

        setupCastListener()

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
                    val casting by remember(isCasting.value) { mutableStateOf(isCasting.value) }
                    if (casting) {
                        CastVideoPlayer(
                            castSession = castSession!!,
                            snackbarHostState = snackbarHostState
                        )
                    } else {
                        VideoPlayer(
                            setPlayer = {
                                exoPlayer = it
                            },
                            refreshPipUi = {
                                if(PipState.mode == PipState.ON){
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
    }

    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    playerViewModel.uiState.collectLatest { uiState ->
                        availableSources = uiState.availableSources
                        selectedSource = uiState.selectedSource
                        episodeUrl = uiState.rawUrl

                        if (castSession != null && castSession!!.isConnected && castSession!!.remoteMediaClient != null) {
                            if (castSession!!.remoteMediaClient!!.mediaInfo == null) {
                                loadRemoteMedia()
                            } else {
                                val castMedia =
                                    castSession!!.remoteMediaClient?.mediaInfo?.customData
                                val episodeId = castMedia?.get("episodeId") as Int
                                val source: StreamSource = json.decodeFromString(
                                    castSession!!.remoteMediaClient!!.mediaInfo!!.customData!!.get("selectedSource") as String
                                )
                                if (episodeId != currentEpisode?.id?.toInt() || source.url != selectedSource?.url || source.fullName != selectedSource?.fullName) {
                                    loadRemoteMedia()
                                }
                            }
                        }
                    }
                }
                launch {
                    playerViewModel.anime.collectLatest { anime ->
                        this@EpisodeActivity.anime = anime
                    }
                }
                launch {
                    playerViewModel.currentEpisode.collectLatest { episode ->
                        if (castSession != null && castSession!!.remoteMediaClient != null) {
                            val castMedia = castSession!!.remoteMediaClient!!.mediaInfo?.customData
                            if (castMedia != null) {
                                val episodeId = castMedia.get("episodeId") as Int
                                val currentEpisodeId = currentEpisode?.id?.toInt()
                                if (currentEpisodeId != null && episodeId != currentEpisodeId) {
                                    stopCastEpisode()
                                }
                            }
                        }
                        currentEpisode = episode
                    }
                }
            }
        }
    }

    override fun onResume() {
        castSessionManagerListener?.let { listener ->
            castContext.sessionManager.addSessionManagerListener(
                listener,
                CastSession::class.java
            )
        }
        castStateListener?.let { castContext.addCastStateListener(it) }
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        castSessionManagerListener?.let { listener ->
            castContext.sessionManager.removeSessionManagerListener(
                listener,
                CastSession::class.java
            )
        }
        castStateListener?.let { castContext.removeCastStateListener(it) }
        castSession = null
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

    fun retryLoadRequest() {
        playerViewModel.setIdleLock(true)
        loadRemoteMedia()
    }

    fun stopCastEpisode(): PendingResult<RemoteMediaClient.MediaChannelResult>? {
        return castSession?.remoteMediaClient?.stop()
    }

    @SuppressLint("VisibleForTests")
    private fun loadRemoteMedia() {
        if (castSession == null || selectedSource == null || currentEpisode == null) {
            return
        }

        val ipv4 = getLocalIPAddress() ?: return
        val remoteMediaClient = castSession!!.remoteMediaClient ?: return

        val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)

        movieMetadata.putString(MediaMetadata.KEY_TITLE, anime?.title ?: "Anime Title")
        movieMetadata.addImage(WebImage(Uri.parse(anime?.thumbnailUrl)))
        movieMetadata.putString(
            MediaMetadata.KEY_SUBTITLE,
            currentEpisode!!.name
        )

        val customData = JSONObject()
            .put("proxyIp", ipv4)
            .put("animeId", currentEpisode!!.animeId)
            .put("episodeId", currentEpisode!!.id)
            .put("seen", currentEpisode!!.seen)

        if (availableSources != null) {
            customData.put("availableSources", json.encodeToString(availableSources))
        }

        customData.put("episodeUrl", episodeUrl)
        customData.put("selectedSource", json.encodeToString(selectedSource))

        var contentUrl = selectedSource!!.url

        if (selectedSource!!.url.substringAfterLast("/")
                .contains(".mp4") && selectedSource!!.headers != null
        ) {
            val proxyUrl = "http://$ipv4:8000"

            val headersString = selectedSource?.headers?.let {
                "&headers=${
                    URLEncoder.encode(
                        json.encodeToString(
                            serializer = OkhttpHeadersSerializer,
                            it
                        ), "UTF-8"
                    )
                }"
            } ?: ""

            contentUrl =
                "$proxyUrl?url=${URLEncoder.encode(selectedSource!!.url, "UTF-8")}$headersString"
        }

        val mediaInfos = MediaInfo.Builder(contentUrl)
            .setContentUrl(contentUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setMetadata(movieMetadata)
            .setCustomData(customData)
            .build()

        stopCastEpisode()

        checkUpdateTimeJobEnded {
            playerViewModel.getDbEpisodeTime { time ->
                remoteMediaClient.load(
                    MediaLoadRequestData.Builder()
                        .setMediaInfo(mediaInfos)
                        .setCurrentTime(time)
                        .setAutoplay(true)
                        .build()
                ).setResultCallback { result ->
                    if (result.status == Status.RESULT_SUCCESS) {
                        playerViewModel.setIdleLock(false)
                    }
                }
            }
        }
    }

    private fun setupCastListener() {
        castStateListener = CastStateListener { state ->
            if (state == CastState.CONNECTING || state == CastState.NOT_CONNECTED) {
                isCasting.value = false
            }
        }

        castSessionManagerListener = object : SessionManagerListener<CastSession> {
            override fun onSessionEnded(session: CastSession, error: Int) {
                onApplicationDisconnected()
            }

            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
                onApplicationConnected(session)
            }

            override fun onSessionResumeFailed(session: CastSession, error: Int) {
                onApplicationDisconnected()
            }

            override fun onSessionStarted(session: CastSession, sessionId: String) {
                onApplicationConnected(session)
            }

            override fun onSessionStartFailed(session: CastSession, error: Int) {
                onApplicationDisconnected()
            }

            override fun onSessionStarting(session: CastSession) {}
            override fun onSessionEnding(session: CastSession) {}
            override fun onSessionResuming(session: CastSession, sessionId: String) {}
            override fun onSessionSuspended(session: CastSession, reason: Int) {}
            private fun onApplicationConnected(session: CastSession) {
                isCasting.value = true
                this@EpisodeActivity.castSession = session
                setCastCustomChannel(session, errorChannel)
                CastProxyService.startNow(this@EpisodeActivity)
                loadRemoteMedia()
            }

            private fun onApplicationDisconnected() {
                isCasting.value = false
                this@EpisodeActivity.castSession = null
            }
        }
    }

    /* PIP MODE HANDLING */

    override fun onStop() {
        /*viewModel.saveCurrentEpisodeWatchingProgress()*/
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
            if (exoPlayer?.isPlaying == false){
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