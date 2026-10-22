package com.sf.tadami.notifications.cast

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.data.episode.EpisodeRepository
import com.sf.tadami.data.interactors.anime.UpdateAnimeInteractor
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.domain.episode.isEpisodeWatched
import com.sf.tadami.domain.episode.toSEpisode
import com.sf.tadami.extension.util.callGuarded
import com.sf.tadami.network.player.PlayerNetworkHelper
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.preferences.advanced.AdvancedPreferences
import com.sf.tadami.preferences.cast.CastPreferences
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.ui.animeinfos.episode.cast.CastProtocol
import com.sf.tadami.ui.animeinfos.episode.cast.CastRemoteState
import com.sf.tadami.ui.animeinfos.episode.cast.CastSessionState
import com.sf.tadami.ui.animeinfos.episode.cast.LocalCast
import com.sf.tadami.ui.animeinfos.episode.cast.buildCastLoadRequest
import com.sf.tadami.ui.animeinfos.episode.cast.castContextOrNull
import com.sf.tadami.ui.animeinfos.episode.cast.channels.ControlChannel
import com.sf.tadami.ui.animeinfos.episode.cast.channels.ErrorChannel
import com.sf.tadami.ui.animeinfos.episode.cast.channels.HandshakeChannel
import com.sf.tadami.ui.animeinfos.episode.cast.channels.ReceiverHandshakeMessage
import com.sf.tadami.ui.animeinfos.episode.cast.channels.TvControlMessage
import com.sf.tadami.ui.animeinfos.episode.cast.channels.toCastSubtitleStyle
import com.sf.tadami.ui.animeinfos.episode.cast.preview.CastPreviewPrefetch
import com.sf.tadami.ui.animeinfos.episode.cast.preview.CastPreviewProtocol
import com.sf.tadami.ui.animeinfos.episode.cast.preview.CastSeekPreview
import com.sf.tadami.ui.animeinfos.episode.cast.proxy.CastProxyHolder
import com.sf.tadami.ui.animeinfos.episode.cast.proxy.CastProxyServer
import com.sf.tadami.ui.animeinfos.episode.cast.sendCastMessage
import com.sf.tadami.ui.animeinfos.episode.cast.setCastCustomChannel
import com.sf.tadami.ui.tabs.browse.SourceManager
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.getPreferencesGroupAsFlow
import com.sf.tadami.utils.isConnectedToWifi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Foreground service that keeps TV-driven cast control (episode switching + watch-time persistence)
 * alive when EpisodeActivity is gone (phone locked / app exited). It is **stateless**: everything a
 * switch needs is rebuilt from the live media's customData (animeId/episodeId/theme/userAgent) plus
 * app-scoped repositories. Started on cast connect, stops itself on session end. Mirrors the former
 * CastProxyService lifecycle; reuses the CastNotifier/CAST_PROXY notification.
 */
class CastControlService : Service() {

    private val sourcesManager: SourceManager = Injekt.get()
    private val episodeRepository: EpisodeRepository = Injekt.get()
    private val animeRepository: AnimeRepository = Injekt.get()
    private val updateAnimeInteractor: UpdateAnimeInteractor = Injekt.get()
    private val dataStore: DataStore<Preferences> = Injekt.get()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    // Null without working Google Play services: the service then stops right away
    private var castContext: CastContext? = null
    private lateinit var notifier: CastNotifier
    private val controlChannel = ControlChannel(
        onPreviewRequest = { seekPreview.onRequest(it) },
        onPreviewSource = { seekPreview.onSource(it) },
    ) { msg -> onControl(msg) }
    private val handshakeChannel = HandshakeChannel { reply -> onHandshakeReply(reply) }
    // Logs receiver-reported errors for the whole session (CastVideoPlayer replaces this callback
    // with its snackbar variant while that screen is open — both paths write to logcat).
    private val errorChannel = ErrorChannel()
    private lateinit var proxyServer: CastProxyServer
    // Seekbar preview frames for the web receiver, cached for this session only
    private lateinit var seekPreview: CastSeekPreview
    private var webNoticeShownThisSession = false
    private var fetchJob: Job? = null
    // Last seen media customData: at end of playback the media goes IDLE/FINISHED and mediaInfo
    // becomes null, so we fall back to this to keep episode switching working past the end.
    private var lastCustomData: JSONObject? = null

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) = bind(session)
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) = bind(session)
        override fun onSessionEnding(session: CastSession) = saveWatchTime(session.remoteMediaClient)
        override fun onSessionEnded(session: CastSession, error: Int) = stopNow()
        override fun onSessionStartFailed(session: CastSession, error: Int) = stopNow()
        override fun onSessionResumeFailed(session: CastSession, error: Int) = stopNow()
        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionSuspended(session: CastSession, reason: Int) {}
    }

    override fun onCreate() {
        super.onCreate()
        castContext = castContextOrNull(applicationContext)
        notifier = CastNotifier(applicationContext)
        // The proxy lives and dies with this service (which spans the whole cast session). Started
        // for every session: it idles for free, and having proxyBaseUrl known before the first load
        // keeps the load path independent of (racy) receiver-type detection.
        proxyServer = CastProxyServer(applicationContext, OkHttpClient.Builder().build())
        CastProxyHolder.server = proxyServer
        seekPreview = CastSeekPreview(
            context = applicationContext,
            playerNetworkHelper = Injekt.get<PlayerNetworkHelper>(),
            send = { message ->
                castContext?.sessionManager?.currentCastSession?.let { sendCastMessage(it, ControlChannel.NAMESPACE, message) }
            },
            localFileUri = proxyServer::localFileUri,
            receiverCapable = { CastPreviewProtocol.CAPABILITY in CastSessionState.receiverCapabilities.value },
            isOnWifi = { applicationContext.isConnectedToWifi() },
            playback = ::castPlayback,
        )
        scope.launch {
            combine(
                dataStore.getPreferencesGroupAsFlow(PlayerPreferences),
                dataStore.getPreferencesGroupAsFlow(AdvancedPreferences),
            ) { player, advanced ->
                CastSeekPreview.Settings(
                    enabled = player.seekPreview,
                    onlyOnWifi = player.seekPreviewOnlyOnWifi,
                    userAgent = advanced.userAgent,
                    prefetch = player.seekPreviewCastPrefetch,
                )
            }.collect { seekPreview.settings = it }
        }
        scope.launch(Dispatchers.IO) {
            // The proxy is stateless; the UA preference is its only phone-side input (applied when
            // neither the request nor its carried source headers set a User-Agent).
            proxyServer.fallbackUserAgent = dataStore.getPreferencesGroup(AdvancedPreferences).userAgent
            if (proxyServer.startSafely()) {
                CastSessionState.proxyBaseUrl.value = proxyServer.baseUrl()
            }
        }
        castContext?.sessionManager?.addSessionManagerListener(sessionListener, CastSession::class.java)
        castContext?.sessionManager?.currentCastSession?.let { bind(it) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notifier.castStatusNotificationBuilder.build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    Notifications.CAST_PROXY_STATUS_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
            } else {
                startForeground(Notifications.CAST_PROXY_STATUS_ID, notification)
            }
        } catch (e: Exception) {
            // e.g. ForegroundServiceStartNotAllowedException — bail out instead of crashing.
            Log.e("CastControlService", "startForeground failed", e)
            stopSelf()
        }
        if (castContext == null) {
            stopNow()
            return START_NOT_STICKY
        }
        // A sticky restart (null intent) after the process died: give the SDK time to resume the
        // session, and stop if none comes back, instead of staying in the foreground with the proxy open.
        if (intent == null) {
            scope.launch {
                delay(STICKY_RESTART_SESSION_WAIT_MS)
                val session = castContext?.sessionManager?.currentCastSession
                if (session == null || !(session.isConnected || session.isConnecting || session.isResuming)) stopNow()
            }
        }
        // Media-notification skip buttons (via CastMediaIntentReceiver) arrive here as actions.
        when (intent?.action) {
            ACTION_SKIP_NEXT -> skip(forward = true)
            ACTION_SKIP_PREV -> skip(forward = false)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        fetchJob?.cancel()
        castContext?.sessionManager?.removeSessionManagerListener(sessionListener, CastSession::class.java)
        // The session's preview frames go with it
        seekPreview.destroy()
        CastProxyHolder.server = null
        proxyServer.stop()
        CastSessionState.clear()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        seekPreview.onTrimMemory(level)
    }

    private fun bind(session: CastSession) {
        setCastCustomChannel(session, controlChannel)
        setCastCustomChannel(session, handshakeChannel)
        setCastCustomChannel(session, errorChannel)
        // Advertise our protocol versions at connect time (before any media load) so the receiver can
        // force an update immediately if it's too old. Re-send shortly after in case the receiver was
        // still starting up and missed the first message.
        sendHandshake(session)
        scope.launch {
            delay(1500)
            sendHandshake(session)
        }
    }

    private fun sendHandshake(session: CastSession) {
        val message = JSONObject()
            .put("senderProtocol", CastProtocol.SENDER_VERSION)
            .put("minReceiverProtocol", CastProtocol.MIN_RECEIVER_VERSION)
            .toString()
        // proxyBaseUrl lets the web receiver build proxied urls pre-load and re-learn the base after
        // a service restart (fresh port). It may still be null on the very first send (async proxy
        // start) — the 1.5s re-send covers that. Native reads only the protocol fields.
        val withProxy = proxyServer.baseUrl()?.let {
            JSONObject(message).put("proxyBaseUrl", it).toString()
        } ?: message
        sendCastMessage(session, HandshakeChannel.NAMESPACE, withProxy)
    }

    /** Only the web receiver replies to the handshake — this is how we detect it. */
    private fun onHandshakeReply(reply: ReceiverHandshakeMessage) {
        if (reply.receiverType != "web") return
        CastSessionState.receiverType.value = CastSessionState.ReceiverType.WEB
        CastSessionState.receiverCapabilities.value = reply.capabilities.toSet()
        if (webNoticeShownThisSession) return
        webNoticeShownThisSession = true
        scope.launch {
            if (dataStore.getPreferencesGroup(CastPreferences).showWebReceiverNotice) {
                CastSessionState.showWebReceiverNotice.value = true
            }
        }
    }

    private fun stopNow() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private val remoteMediaClient: RemoteMediaClient?
        get() = castContext?.sessionManager?.currentCastSession?.remoteMediaClient

    /** The TV's player state and position, for the seekbar preview prefetch. */
    private fun castPlayback(): CastSeekPreview.CastPlayback? {
        val client = remoteMediaClient ?: return null
        val state = when (client.playerState) {
            MediaStatus.PLAYER_STATE_PLAYING -> CastPreviewPrefetch.Playback.PLAYING
            MediaStatus.PLAYER_STATE_PAUSED -> CastPreviewPrefetch.Playback.PAUSED
            MediaStatus.PLAYER_STATE_BUFFERING -> CastPreviewPrefetch.Playback.BUFFERING
            MediaStatus.PLAYER_STATE_LOADING -> CastPreviewPrefetch.Playback.LOADING
            MediaStatus.PLAYER_STATE_IDLE -> CastPreviewPrefetch.Playback.IDLE
            else -> null
        }
        return CastSeekPreview.CastPlayback(state, client.approximateStreamPosition)
    }

    /** Control messages from the TV: persist watch time, switch episodes, or mirror TV-local track selection. */
    private fun onControl(msg: TvControlMessage) {
        if (msg.type == "state") {
            // TV-local source/subtitle/audio selection — publish for the phone UI (PlayerViewModel) to mirror.
            CastRemoteState.selection.value = CastRemoteState.Selection(
                sourceIndex = msg.sourceIndex,
                subtitleIndex = msg.subtitleIndex,
                audioIndex = msg.audioIndex,
            )
            return
        }
        val liveCustomData = remoteMediaClient?.mediaInfo?.customData
        if (liveCustomData != null) lastCustomData = liveCustomData
        val customData = liveCustomData ?: lastCustomData ?: return
        when (msg.type) {
            "progress", "save" -> saveTime(episodeFromCustomData(customData), msg.duration, msg.position)
            "next" -> switchNeighbour(customData, forward = true)
            "previous" -> switchNeighbour(customData, forward = false)
            "selectEpisode" -> msg.episodeId?.let { targetId ->
                scope.launch {
                    val target = runCatching { episodeRepository.getEpisodeById(targetId) }.getOrNull()
                    if (target != null) switchEpisode(customData, target)
                }
            }
        }
    }

    /** Media-notification skip: resolve the live (or last-seen) customData and switch episode. */
    private fun skip(forward: Boolean) {
        val liveCustomData = remoteMediaClient?.mediaInfo?.customData
        if (liveCustomData != null) lastCustomData = liveCustomData
        val customData = liveCustomData ?: lastCustomData ?: return
        switchNeighbour(customData, forward)
    }

    private fun switchNeighbour(customData: JSONObject, forward: Boolean) {
        val animeId = (customData.get("animeId") as? Number)?.toLong() ?: return
        val currentId = (customData.get("episodeId") as? Number)?.toLong() ?: return
        scope.launch {
            val episodes = runCatching { episodeRepository.getEpisodesByAnimeId(animeId) }
                .getOrNull()?.sortedBy { it.sourceOrder } ?: return@launch
            val index = episodes.indexOfFirst { it.id == currentId }
            if (index < 0) return@launch
            // Mirror the phone iterator: "next" = the entry before the current one.
            val target = if (forward) episodes.getOrNull(index - 1) else episodes.getOrNull(index + 1)
            if (target != null) switchEpisode(customData, target)
        }
    }

    /** Re-resolve [target]'s sources app-scoped and load it onto the receiver at its saved time. */
    @OptIn(UnstableApi::class)
    private suspend fun switchEpisode(customData: JSONObject, target: Episode) {
        // Persist the outgoing episode's time first.
        saveTime(
            episodeFromCustomData(customData),
            remoteMediaClient?.streamDuration ?: 0L,
            remoteMediaClient?.approximateStreamPosition ?: 0L,
        )

        val anime: Anime = runCatching { animeRepository.getAnimeById(target.animeId) }.getOrNull() ?: return
        val fresh = runCatching { episodeRepository.getEpisodeById(target.id) }.getOrNull() ?: target
        val episodes = runCatching { episodeRepository.getEpisodesByAnimeId(target.animeId) }
            .getOrNull()?.sortedBy { it.sourceOrder } ?: listOf(fresh)
        val playerPrefs = dataStore.getPreferencesGroup(PlayerPreferences)
        // Read the UA preference directly (not the customData echo of it) — fresher if the user
        // edited it mid-session, and it keeps both load paths identical.
        val userAgent = dataStore.getPreferencesGroup(AdvancedPreferences).userAgent
        val themeJson = if (customData.has("theme")) customData.optString("theme").ifBlank { null } else null
        val displayMode = if (anime.displayMode is Anime.DisplayMode.NAME) "NAME" else "NUMBER"
        val resume = if (fresh.seen) 0L else fresh.timeSeen
        val subtitlePrefs = playerPrefs.subtitlePrefLanguages.split(",").filter { it.isNotBlank() }
        val source = sourcesManager.getOrStub(anime.source)
        fun request(selected: StreamSource, sources: List<StreamSource>, contentUrl: String = selected.url) =
            buildCastLoadRequest(
                episode = fresh,
                anime = anime,
                availableSources = sources,
                selectedSource = selected,
                episodeUrl = fresh.url,
                episodes = episodes,
                displayMode = displayMode,
                themeJson = themeJson,
                userAgent = userAgent,
                subtitlePrefLanguages = subtitlePrefs,
                subtitlesEnabled = playerPrefs.subtitlesEnabled,
                resumeTimeMs = resume,
                proxyBaseUrl = proxyServer.baseUrl(),
                subtitleStyle = playerPrefs.toCastSubtitleStyle(),
                contentUrl = contentUrl,
                seekPreview = playerPrefs.seekPreview,
            )

        fetchJob?.cancel()
        // A downloaded episode is served by the phone, when the receiver can play its files
        val local = LocalCast.resolve(applicationContext, fresh, proxyServer)
        if (local != null) {
            remoteMediaClient?.load(request(local.source, listOf(local.source), local.contentUrl))
            return
        }
        fetchJob = scope.launch {
            val sources = try {
                source.callGuarded { getEpisodeSources(fresh.toSEpisode()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // fetch error: leave current playback as-is
                return@launch
            }
            val selected = sources.firstOrNull() ?: return@launch
            remoteMediaClient?.load(request(selected, sources))
        }
    }

    private fun episodeFromCustomData(customData: JSONObject): Episode {
        val id = (customData.get("episodeId") as? Number)?.toLong() ?: 0L
        val seen = customData.optBoolean("seen", false)
        return Episode.create().copy(id = id, seen = seen)
    }

    @OptIn(UnstableApi::class)
    private fun saveTime(episode: Episode, totalTime: Long, timeSeen: Long) {
        scope.launch {
            val threshold = dataStore.getPreferencesGroup(PlayerPreferences).seenThreshold
            if (episode.seen || episode.id == 0L) return@launch
            if (totalTime > 0L && timeSeen > 999L) {
                withContext(Dispatchers.IO) {
                    val watched = isEpisodeWatched(timeSeen, totalTime, threshold)
                    if (watched) {
                        updateAnimeInteractor.awaitSeenEpisodeUpdate(setOf(episode.id), true)
                    } else {
                        updateAnimeInteractor.awaitSeenEpisodeTimeUpdate(episode, totalTime, timeSeen)
                    }
                }
            }
        }
    }

    private fun saveWatchTime(client: RemoteMediaClient?) {
        val customData = client?.mediaInfo?.customData ?: return
        saveTime(episodeFromCustomData(customData), client.streamDuration, client.approximateStreamPosition)
    }

    companion object {
        private const val ACTION_SKIP_NEXT = "com.sf.tadami.cast.SKIP_NEXT"
        private const val ACTION_SKIP_PREV = "com.sf.tadami.cast.SKIP_PREV"
        private const val STICKY_RESTART_SESSION_WAIT_MS = 15_000L

        /** Called from the media-notification skip buttons to switch episode forward/back. */
        fun skipEpisode(context: Context, forward: Boolean) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, CastControlService::class.java)
                        .setAction(if (forward) ACTION_SKIP_NEXT else ACTION_SKIP_PREV),
                )
            }.onFailure { Log.e("CastControlService", "skipEpisode failed", it) }
        }

        fun startNow(context: Context) {
            // A disallowed foreground-service start (background restrictions on modern Android) must
            // degrade gracefully instead of crashing the app.
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, CastControlService::class.java))
            }.onFailure { Log.e("CastControlService", "start failed", it) }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, CastControlService::class.java)) }
        }
    }
}
