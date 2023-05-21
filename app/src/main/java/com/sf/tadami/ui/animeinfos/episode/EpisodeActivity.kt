package com.sf.tadami.ui.animeinfos.episode

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.notifications.cast.CastProxyService
import com.sf.tadami.ui.animeinfos.episode.cast.getLocalIPAddress
import com.sf.tadami.ui.animeinfos.episode.player.VideoPlayer
import com.sf.tadami.ui.themes.TadamiTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject

class EpisodeActivity : AppCompatActivity() {
    private var castSession: CastSession? = null
    private lateinit var castContext: CastContext
    private var castSessionManagerListener: SessionManagerListener<CastSession>? = null
    private var availableSources: List<StreamSource>? = null
    private var selectedSource: StreamSource? = null
    private var animeTitle: String? = null
    private var currentEpisode: Episode? = null

    private val playerViewModel: PlayerViewModel by viewModels()

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

        setupCastListener()

        observeData()

        setContent {
            TadamiTheme {
                VideoPlayer()
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
                        if (castSession?.isConnected == true && availableSources!!.isNotEmpty()) {
                            loadRemoteMedia(true)
                        }
                    }
                }
                launch {
                    playerViewModel.animeTitle.collectLatest { title ->
                        animeTitle = title
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

    private fun loadRemoteMedia(autoPlay: Boolean) {
        if (castSession == null || selectedSource == null || currentEpisode == null) {
            return
        }

        val ipv4 = getLocalIPAddress() ?: return
        val remoteMediaClient = castSession!!.remoteMediaClient ?: return

        val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)

        movieMetadata.putString(MediaMetadata.KEY_TITLE, animeTitle ?: "Anime Title")
       /* movieMetadata.addImage(WebImage(Uri.parse("https://static.actu.fr/uploads/2022/04/cbl-1-960x640.jpg")))*/
        movieMetadata.putString(
            MediaMetadata.KEY_SUBTITLE,
            currentEpisode!!.name
        )

        val customData = JSONObject()
            .put("proxyIp", ipv4)
            .put("animeId", currentEpisode!!.animeId)
            .put("episodeId", currentEpisode!!.id)

        if (selectedSource!!.headers != null) {
            val headers = JSONObject()
            selectedSource!!.headers!!.forEach { (key, value) ->
                headers.put(key, value)
            }
            customData.put("headers", headers)
        }

        val mediaInfos = MediaInfo.Builder(selectedSource!!.url)
            .setContentUrl(selectedSource!!.url)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setMetadata(movieMetadata)
            .setCustomData(customData)
            .build()

        remoteMediaClient.load(
            MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfos)
                .setCurrentTime(currentEpisode?.timeSeen ?: 0)
                .setAutoplay(autoPlay)
                .build()
        )
    }

    private fun setupCastListener() {
        castSessionManagerListener = object : SessionManagerListener<CastSession> {
            override fun onSessionEnded(session: CastSession, error: Int) {
                onApplicationDisconnected(session)
            }

            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
                onApplicationConnected(session)
            }

            override fun onSessionResumeFailed(session: CastSession, error: Int) {
                onApplicationDisconnected(session)
            }

            override fun onSessionStarted(session: CastSession, sessionId: String) {
                onApplicationConnected(session)
            }

            override fun onSessionStartFailed(session: CastSession, error: Int) {
                onApplicationDisconnected(session)
            }

            override fun onSessionStarting(session: CastSession) {}
            override fun onSessionEnding(session: CastSession) {}
            override fun onSessionResuming(session: CastSession, sessionId: String) {}
            override fun onSessionSuspended(session: CastSession, reason: Int) {}
            private fun onApplicationConnected(castSession: CastSession) {
                CastProxyService.startNow(this@EpisodeActivity)
                this@EpisodeActivity.castSession = castSession
                loadRemoteMedia(true)
            }

            private fun onApplicationDisconnected(session: CastSession) {
                CastProxyService.stop(this@EpisodeActivity)
                this@EpisodeActivity.castSession = null
            }
        }
    }
}