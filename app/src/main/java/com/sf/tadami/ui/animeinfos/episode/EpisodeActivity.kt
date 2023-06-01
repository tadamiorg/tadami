package com.sf.tadami.ui.animeinfos.episode

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.common.images.WebImage
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.notifications.cast.CastProxyService
import com.sf.tadami.ui.animeinfos.episode.cast.channels.SeekChannel
import com.sf.tadami.ui.animeinfos.episode.cast.getLocalIPAddress
import com.sf.tadami.ui.animeinfos.episode.cast.setCastCustomChannel
import com.sf.tadami.ui.animeinfos.episode.player.CastVideoPlayer
import com.sf.tadami.ui.animeinfos.episode.player.VideoPlayer
import com.sf.tadami.ui.themes.TadamiTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject

class EpisodeActivity : AppCompatActivity() {

    private val json: Json = Json


    private var castSession: CastSession? = null
    private val isCasting = mutableStateOf(false)
    private lateinit var castContext: CastContext
    private var castSessionManagerListener: SessionManagerListener<CastSession>? = null
    private var availableSources: List<StreamSource>? = null
    private var selectedSource: StreamSource? = null
    private var episodeUrl: String? = null
    private var anime: Anime? = null
    private var currentEpisode: Episode? = null
    private var updateTimeJob: Job? = null
    private val seekChannel = SeekChannel()

    private lateinit var playerViewModel: PlayerViewModel

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
            TadamiTheme {
                val casting by isCasting
                if (casting) {
                    CastVideoPlayer(castSession = castSession!!)
                } else {
                    VideoPlayer()
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
                                if (episodeId != currentEpisode?.id?.toInt() || source.url != selectedSource?.url) {
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
                        currentEpisode = episode
                    }
                }
            }
        }
    }

    override fun onResume() {
        castContext.sessionManager.addSessionManagerListener(
            castSessionManagerListener!!,
            CastSession::class.java
        )
        isCasting.value = !(castSession != null && castSession!!.isDisconnected)
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        castContext.sessionManager.removeSessionManagerListener(
            castSessionManagerListener!!,
            CastSession::class.java
        )
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
                Log.e("Error Update Time : ", cancellation.message.toString())
            }
        }
    }

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

        if (availableSources != null) {
            customData.put("availableSources", json.encodeToString(availableSources))
        }

        customData.put("episodeUrl", episodeUrl)

        customData.put("selectedSource", json.encodeToString(selectedSource))

        val mediaInfos = MediaInfo.Builder(selectedSource!!.url)
            .setContentUrl(selectedSource!!.url)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setMetadata(movieMetadata)
            .setCustomData(customData)
            .build()

        remoteMediaClient.stop()

        checkUpdateTimeJobEnded {
            playerViewModel.getDbEpisodeTime { time ->
                remoteMediaClient.load(
                    MediaLoadRequestData.Builder()
                        .setMediaInfo(mediaInfos)
                        .setCurrentTime(time)
                        .setAutoplay(true)
                        .build()
                )
            }
        }
    }

    private fun setupCastListener() {
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
                setCastCustomChannel(session, seekChannel)
                CastProxyService.startNow(this@EpisodeActivity)
                loadRemoteMedia()
            }

            private fun onApplicationDisconnected() {
                isCasting.value = false
                CastProxyService.stop(this@EpisodeActivity)
                this@EpisodeActivity.castSession = null
            }
        }
    }
}