package com.sf.tadami.ui.animeinfos.episode

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
    import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaTrack
import com.google.android.gms.cast.TextTrackStyle
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.images.WebImage
import com.sf.tadami.R
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.domain.episode.toSEpisode
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.source.model.OkhttpHeadersSerializer
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.source.model.Track
import com.sf.tadami.source.online.AnimeHttpSource
import com.sf.tadami.ui.animeinfos.episode.cast.channels.CrashChannel
import com.sf.tadami.ui.animeinfos.episode.cast.channels.ErrorChannel
import com.sf.tadami.ui.animeinfos.episode.cast.channels.TvCrashLog
import com.sf.tadami.notifications.cast.CastControlService
import com.sf.tadami.ui.animeinfos.episode.cast.CastConnectionErrorDialog
import com.sf.tadami.ui.animeinfos.episode.cast.getLocalIPAddress
import com.sf.tadami.ui.animeinfos.episode.cast.logCastConnectionError
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
import com.sf.tadami.ui.utils.convertToIetfLanguageTag
import com.sf.tadami.ui.utils.getUriCompat
import com.sf.tadami.ui.utils.setComposeContent
import com.sf.tadami.ui.utils.toShareIntent
import com.sf.tadami.ui.webview.WebViewActivity
import com.sf.tadami.utils.createFileInCacheDir
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.getPreferencesGroupAsFlow
import com.sf.tadami.preferences.advanced.AdvancedPreferences
import kotlinx.coroutines.runBlocking
import com.sf.tadami.utils.powerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


@UnstableApi
class EpisodeActivity : AppCompatActivity() {

    private val json: Json = Json
    private var castSession: CastSession? = null
    private val isCasting = mutableStateOf(false)
    private lateinit var castContext: CastContext
    private var castSessionManagerListener: SessionManagerListener<CastSession>? = null
    private var castStateListener: CastStateListener? = null
    private var availableSources: List<StreamSource>? = null
    private var selectedSource: StreamSource? = null
    private var selectedTextTrack: Track.SubtitleTrack? = null
    private var episodeUrl: String? = null
    private var anime: Anime? = null
    private var currentEpisode: Episode? = null
    private var updateTimeJob: Job? = null
    private val errorChannel = ErrorChannel()
    private val crashChannel = CrashChannel { log -> onTvCrashReceived(log) }
    private val tvCrashLog = mutableStateOf<String?>(null)
    private val castConnectionError = mutableStateOf(false)
    private var activeColorScheme: ColorScheme? = null
    private var pipReceiver: BroadcastReceiver? = null
    private var exoPlayer: ExoPlayer? = null
    private val dataStore: DataStore<Preferences> = Injekt.get()
    private var playerPreferences: PlayerPreferences? = null

    private lateinit var playerViewModel: PlayerViewModel

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES;
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        overridePendingTransition(R.anim.shared_axis_x_push_enter, R.anim.shared_axis_x_push_exit)
        super.onCreate(savedInstanceState)

        castContext = CastContext.getSharedInstance(this)
        castSession = castContext.sessionManager.currentCastSession
        isCasting.value = castSession?.isConnected ?: false
        if (castSession?.remoteMediaClient?.mediaInfo?.customData != null) {
            try {
                val selectedTracks =
                    castSession?.remoteMediaClient?.mediaStatus?.activeTrackIds?.takeIf { it.isNotEmpty() }
                        ?.get(0)
                val customDataSources =
                    castSession?.remoteMediaClient?.mediaInfo?.customData!!.get("availableSources") as String
                val selectedSource =
                    castSession?.remoteMediaClient?.mediaInfo?.customData!!.get("selectedSource") as String

                val rawUrl =
                    castSession?.remoteMediaClient?.mediaInfo?.customData!!.get("episodeUrl") as String
                val episodeId =
                    (castSession?.remoteMediaClient?.mediaInfo?.customData!!.get("episodeId") as? Number)?.toInt()
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
                        ),
                        selectedTrack = selectedTracks
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

        setComposeContent(
            isDark = true,
            amoled = true
        ) {
            val statusBarBackgroundColor = MaterialTheme.colorScheme.surface
            val isSystemInDarkTheme = isSystemInDarkTheme()

            // Capture the active app color scheme so it can be forwarded to the TV receiver.
            val appColorScheme = MaterialTheme.colorScheme
            LaunchedEffect(appColorScheme) { activeColorScheme = appColorScheme }

            LaunchedEffect(isSystemInDarkTheme, statusBarBackgroundColor) {
                // Draw edge-to-edge and set system bars color to transparent
                val lightStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK)
                val darkStyle = SystemBarStyle.dark(Color.TRANSPARENT)
                enableEdgeToEdge(
                    statusBarStyle = if (statusBarBackgroundColor.luminance() > 0.5) lightStyle else darkStyle,
                    navigationBarStyle = if (isSystemInDarkTheme) darkStyle else lightStyle,
                )
            }

            val snackbarHostState = remember { SnackbarHostState() }
            Scaffold(
                snackbarHost = {
                    SnackbarHost(
                        modifier = Modifier.padding(bottom = 100.dp),
                        hostState = snackbarHostState
                    )
                }
            ) {
                Box {
                    val casting by remember(isCasting.value) { mutableStateOf(isCasting.value) }

                    if (castConnectionError.value) {
                        CastConnectionErrorDialog(
                            onDismissRequest = { castConnectionError.value = false }
                        )
                    }

                    tvCrashLog.value?.let { crashText ->
                        AlertDialog(
                            onDismissRequest = { tvCrashLog.value = null },
                            title = { Text("Tadami TV crashed") },
                            text = { Text("The TV reported a crash. Export the error log?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    shareCrashLog(crashText)
                                    tvCrashLog.value = null
                                }) { Text("Export") }
                            },
                            dismissButton = {
                                TextButton(onClick = { tvCrashLog.value = null }) { Text("Dismiss") }
                            },
                        )
                    }

                    if (casting) {
                        CastVideoPlayer(
                            castSession = castSession!!,
                            snackbarHostState = snackbarHostState,
                            onWebViewOpen = {
                                val httpSource = playerViewModel.source as? AnimeHttpSource
                                val url = playerViewModel.currentEpisode.value?.let {
                                    httpSource?.getEpisodeUrl(it.toSEpisode())
                                }
                                val title = playerViewModel.anime.value?.title
                                if (httpSource != null && url != null && title != null) {
                                    val encodedUrl = URLEncoder.encode(
                                        url,
                                        StandardCharsets.UTF_8.toString()
                                    )
                                    openEpisodeInWebView(
                                        url = encodedUrl,
                                        sourceId = httpSource.id,
                                        animeTitle = title
                                    )
                                }
                            }
                        )
                    } else {
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
                            },
                            onWebViewOpen = {
                                val httpSource = playerViewModel.source as? AnimeHttpSource
                                val url = playerViewModel.currentEpisode.value?.let {
                                    httpSource?.getEpisodeUrl(it.toSEpisode())
                                }
                                val title = playerViewModel.anime.value?.title
                                if (httpSource != null && url != null && title != null) {
                                    val encodedUrl = URLEncoder.encode(
                                        url,
                                        StandardCharsets.UTF_8.toString()
                                    )
                                    openEpisodeInWebView(
                                        url = encodedUrl,
                                        sourceId = httpSource.id,
                                        animeTitle = title
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun openEpisodeInWebView(url: String, sourceId: Long, animeTitle: String) {
        val intent = WebViewActivity.newIntent(this@EpisodeActivity, url, sourceId, animeTitle)
        startActivity(intent)
    }

    private fun observeData() {
        // NOTE: these collectors are intentionally NOT wrapped in repeatOnLifecycle(STARTED). Episode
        // swapping is driven from the TV while the phone is backgrounded/screen-off (not STARTED); the
        // uiState -> loadRemoteMedia() push and the currentEpisode mirror must keep running then, or the
        // receiver never gets the new episode. They run until onDestroy (lifecycleScope). No UI work is
        // done here (loadRemoteMedia only touches Cast + plain fields), so running while stopped is safe.
        lifecycleScope.launch {
            dataStore.getPreferencesGroupAsFlow(PlayerPreferences).collectLatest {
                playerPreferences = it
            }
        }
        lifecycleScope.launch {
            playerViewModel.uiState.collectLatest { uiState ->
                availableSources = uiState.availableSources
                selectedSource = uiState.selectedSource
                episodeUrl = uiState.rawUrl
                selectedTextTrack = uiState.selectedSubtitleTrack

                val remoteMediaClient = castSession?.takeIf { it.isConnected }?.remoteMediaClient
                if (remoteMediaClient != null) {
                    // Capture mediaInfo once: stopCastEpisode() unloads media asynchronously, so it can
                    // go null between a check and a re-read (TOCTOU) and NPE this collector.
                    val mediaInfo = remoteMediaClient.mediaInfo
                    val castMedia = mediaInfo?.customData
                    if (castMedia == null) {
                        loadRemoteMedia()
                    } else {
                        // episodeId round-trips as Integer (small ids) or Long (large) — read as Number
                        // so a large id doesn't throw and kill this collector.
                        val episodeId = (castMedia.get("episodeId") as? Number)?.toInt()
                        val source: StreamSource = json.decodeFromString(
                            castMedia.get("selectedSource") as String
                        )
                        if (episodeId != currentEpisode?.id?.toInt() || source.url != selectedSource?.url || source.fullName != selectedSource?.fullName) {
                            loadRemoteMedia()
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            playerViewModel.anime.collectLatest { anime ->
                this@EpisodeActivity.anime = anime
            }
        }
        lifecycleScope.launch {
            playerViewModel.currentEpisode.collectLatest { episode ->
                val remoteMediaClient = castSession?.remoteMediaClient
                val castMedia = remoteMediaClient?.mediaInfo?.customData
                if (castMedia != null) {
                    val episodeId = (castMedia.get("episodeId") as? Number)?.toInt()
                    val currentEpisodeId = currentEpisode?.id?.toInt()
                    if (currentEpisodeId != null && episodeId != null && episodeId != currentEpisodeId) {
                        stopCastEpisode()
                    }
                }
                currentEpisode = episode
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
        // addSessionManagerListener does NOT replay onSessionStarted/Resumed for a session that is
        // already live, so when we open onto an existing cast session register the (activity-scoped)
        // error/crash channels here and make sure the control service is running (it owns the control
        // channel + episode switching, which must survive the activity being locked/exited).
        castContext.sessionManager.currentCastSession?.takeIf { it.isConnected }?.let { session ->
            castSession = session
            setCastCustomChannel(session, errorChannel)
            setCastCustomChannel(session, crashChannel)
            CastControlService.startNow(this)
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

    /** A crash log pushed by the TV receiver: persist a copy and surface the export dialog. */
    private fun onTvCrashReceived(log: TvCrashLog) {
        val text = buildString {
            appendLine("Tadami TV crash report")
            appendLine("Package: ${log.packageName}")
            appendLine("Version: ${log.versionName}")
            appendLine("Time: ${java.util.Date(log.timestamp)}")
            appendLine()
            append(log.stacktrace)
        }
        runCatching {
            val dir = java.io.File(getExternalFilesDir(null), "tv-crash").apply { mkdirs() }
            java.io.File(dir, "tv_crash_${log.timestamp}.txt").writeText(text)
        }
        runOnUiThread { tvCrashLog.value = text }
    }

    private fun shareCrashLog(text: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val file = createFileInCacheDir("tadami_tv_crash_logs.txt")
                file.writeText(text)
                val uri = file.getUriCompat(this@EpisodeActivity)
                withContext(Dispatchers.Main) {
                    startActivity(uri.toShareIntent(this@EpisodeActivity, "text/plain"))
                }
            }
        }
    }

    @SuppressLint("VisibleForTests")
    private fun loadRemoteMedia() {
        // Use the ViewModel's current episode (the source of truth that drove the switch), not the
        // Activity mirror field which can be stale on a swap — otherwise the receiver gets the new
        // video with the previous episode's title/duration.
        val episode = playerViewModel.currentEpisode.value ?: currentEpisode
        // Same reasoning: use the ViewModel's anime, not the Activity field which can be null when the
        // collectors haven't repopulated it yet on return (a null thumbnailUrl NPEs Uri.parse).
        val anime = playerViewModel.anime.value ?: this.anime
        if (castSession == null || selectedSource == null || episode == null) {
            return
        }

        val remoteMediaClient = castSession!!.remoteMediaClient ?: return

        val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)

        movieMetadata.putString(MediaMetadata.KEY_TITLE, anime?.title ?: "Anime Title")
        anime?.thumbnailUrl?.let { movieMetadata.addImage(WebImage(Uri.parse(it))) }
        movieMetadata.putString(
            MediaMetadata.KEY_SUBTITLE,
            episode.name
        )

        val customData = JSONObject()
            .put("userAgent", runBlocking { dataStore.getPreferencesGroup(AdvancedPreferences) }.userAgent)
            .put("animeId", episode.animeId)
            .put("episodeId", episode.id)
            .put("seen", episode.seen)

        if (availableSources != null) {
            customData.put("availableSources", json.encodeToString(availableSources))
        }

        customData.put("episodeUrl", episodeUrl)
        customData.put("selectedSource", json.encodeToString(selectedSource))

        // Episode list + display mode so the TV can offer episode navigation.
        val episodesArray = JSONArray()
        playerViewModel.episodes.value.forEach { ep ->
            episodesArray.put(
                JSONObject()
                    .put("id", ep.id)
                    .put("name", ep.name)
                    .put("episodeNumber", ep.episodeNumber.toDouble())
                    .put("seen", ep.seen)
            )
        }
        customData.put("episodes", episodesArray.toString())
        customData.put(
            "displayMode",
            if (anime?.displayMode is Anime.DisplayMode.NAME) "NAME" else "NUMBER"
        )

        // Forward the active app color scheme so the TV mirrors the phone's theme.
        activeColorScheme?.let { cs ->
            customData.put(
                "theme",
                JSONObject()
                    .put("primary", cs.primary.toArgb())
                    .put("onPrimary", cs.onPrimary.toArgb())
                    .put("secondary", cs.secondary.toArgb())
                    .put("onSecondary", cs.onSecondary.toArgb())
                    .put("background", cs.background.toArgb())
                    .put("onBackground", cs.onBackground.toArgb())
                    .put("surface", cs.surface.toArgb())
                    .put("onSurface", cs.onSurface.toArgb())
                    .put("surfaceVariant", cs.surfaceVariant.toArgb())
                    .put("onSurfaceVariant", cs.onSurfaceVariant.toArgb())
                    .toString()
            )
        }

        // No proxy: send the raw stream URL. The native Tadami-TV receiver injects the
        // source headers itself (sent in selectedSource customData), like the phone player.
        val contentUrl = selectedSource!!.url

        val mediaInfosBuilder = MediaInfo.Builder(contentUrl)
            .setContentUrl(contentUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setMetadata(movieMetadata)
            .setCustomData(customData)

        // Known-duration fallback (re-watched episodes) so total time shows immediately; for
        // first watches the receiver reports the real duration via its MediaStatus.
        episode.totalTime.takeIf { it > 0L }?.let {
            mediaInfosBuilder.setStreamDuration(it)
        }

        var activeTrackIds: LongArray? = null

        // Add captions/subtitles if available
        selectedSource?.subtitleTracks?.let { subtitleTracks ->
            if (subtitleTracks.isNotEmpty()) {
                val mediaTracks = ArrayList<MediaTrack>()

                val trackNameMap = subtitleTracks.groupBy { it.lang }
                    .flatMap { (lang, tracks) ->
                        if (tracks.size > 1) {
                            // If there are multiple tracks with the same language, add numbers
                            tracks.mapIndexed { index, track ->
                                track to "$lang #${index + 1}"
                            }
                        } else {
                            // If there's only one track with this language, use the language name as is
                            tracks.map { it to lang }
                        }
                    }.toMap()

                // Add each subtitle track with a unique ID
                subtitleTracks.forEachIndexed { index, track ->
                    val trackId = (index + 1).toLong() // Track IDs should start from 1
                    val trackName = trackNameMap[track] ?: "Subtitle ${index + 1}"

                    val subtitle = MediaTrack.Builder(trackId, MediaTrack.TYPE_TEXT)
                        .setName(trackName)
                        .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
                        .setContentId(track.url)
                        .setLanguage(track.lang.convertToIetfLanguageTag())
                        .setContentType(track.mimeType)
                        .build()

                    mediaTracks.add(subtitle)
                }

                if (mediaTracks.isNotEmpty()) {
                    var selectedTrackId: Long? = null
                    val userPreferredLanguages =
                        playerPreferences?.subtitlePrefLanguages?.split(",")

                    if (!userPreferredLanguages.isNullOrEmpty()) {
                        // Try each preferred language in order until we find a match
                        for (lang in userPreferredLanguages) {
                            val matchIndex =
                                subtitleTracks.indexOfFirst { it.lang.convertToIetfLanguageTag() == lang }
                            if (matchIndex >= 0) {
                                selectedTrackId = matchIndex + 1L
                                break
                            }
                        }
                    }

                    // If no preferred language match was found, fall back to the first track
                    activeTrackIds = if (selectedTrackId != null) {
                        longArrayOf(selectedTrackId)
                    } else {
                        longArrayOf(1) // Default to first track if no preferred language match
                    }

                    mediaInfosBuilder.setMediaTracks(mediaTracks)
                }
            }
        }

        val textTrackStyle = TextTrackStyle.fromSystemSettings(this).apply {
            foregroundColor = Color.argb(255, 255, 255, 255)        // 0xFFFFFFFF
            backgroundColor = Color.argb(0, 0, 0, 1)
            edgeType = TextTrackStyle.EDGE_TYPE_OUTLINE
            edgeColor = Color.argb(240, 0, 0, 0)
            fontStyle = TextTrackStyle.FONT_STYLE_BOLD

        }

        mediaInfosBuilder.setTextTrackStyle(textTrackStyle)

        val mediaInfos = mediaInfosBuilder.build()

        val mediaLoadRequestBuilder = MediaLoadRequestData.Builder()
            .setAutoplay(true)

        activeTrackIds?.let { trackIds ->
            mediaLoadRequestBuilder.setActiveTrackIds(trackIds)
            playerViewModel.selectedSubtitleTrack(
                playerViewModel.uiState.value.selectedSource?.subtitleTracks?.takeIf { it.isNotEmpty() }
                    ?.get(
                        (trackIds[0] - 1L).toInt()
                    )
            )
        }

        mediaLoadRequestBuilder.setMediaInfo(mediaInfos)

        stopCastEpisode()

        checkUpdateTimeJobEnded {
            playerViewModel.getDbEpisodeTime { time ->
                remoteMediaClient.load(
                    mediaLoadRequestBuilder.setCurrentTime(time).build()
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
                logCastConnectionError("Session resume", error)
                castConnectionError.value = true
                onApplicationDisconnected()
            }

            override fun onSessionStarted(session: CastSession, sessionId: String) {
                onApplicationConnected(session)
            }

            override fun onSessionStartFailed(session: CastSession, error: Int) {
                logCastConnectionError("Session start", error)
                castConnectionError.value = true
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
                setCastCustomChannel(session, crashChannel)
                // The foreground service owns the control channel + episode switching so it survives
                // the activity being locked/exited.
                CastControlService.startNow(this@EpisodeActivity)
                loadRemoteMedia()
            }

            private fun onApplicationDisconnected() {
                // The CastControlService flushes the final watch time on session end and self-stops.
                isCasting.value = false
                this@EpisodeActivity.castSession = null
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.shared_axis_x_pop_enter, R.anim.shared_axis_x_pop_exit)
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
                registerReceiver(
                    pipReceiver, IntentFilter(ACTION_MEDIA_CONTROL),
                    RECEIVER_NOT_EXPORTED
                )
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