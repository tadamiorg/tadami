package com.sf.tadami.notifications.cast

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.data.episode.EpisodeRepository
import com.sf.tadami.data.interactors.anime.UpdateAnimeInteractor
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.preferences.advanced.AdvancedPreferences
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.ui.animeinfos.episode.cast.buildCastLoadRequest
import com.sf.tadami.ui.animeinfos.episode.cast.channels.ControlChannel
import com.sf.tadami.ui.animeinfos.episode.cast.channels.TvControlMessage
import com.sf.tadami.ui.animeinfos.episode.cast.setCastCustomChannel
import com.sf.tadami.ui.tabs.browse.SourceManager
import com.sf.tadami.utils.getPreferencesGroup
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private lateinit var castContext: CastContext
    private lateinit var notifier: CastNotifier
    private val controlChannel = ControlChannel { msg -> onControl(msg) }
    private var fetchDisposable: Disposable? = null
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
        castContext = CastContext.getSharedInstance(applicationContext)
        notifier = CastNotifier(applicationContext)
        castContext.sessionManager.addSessionManagerListener(sessionListener, CastSession::class.java)
        castContext.sessionManager.currentCastSession?.let { bind(it) }
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
        // Media-notification skip buttons (via CastMediaIntentReceiver) arrive here as actions.
        when (intent?.action) {
            ACTION_SKIP_NEXT -> skip(forward = true)
            ACTION_SKIP_PREV -> skip(forward = false)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        fetchDisposable?.dispose()
        castContext.sessionManager.removeSessionManagerListener(sessionListener, CastSession::class.java)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun bind(session: CastSession) {
        setCastCustomChannel(session, controlChannel)
    }

    private fun stopNow() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private val remoteMediaClient: RemoteMediaClient?
        get() = castContext.sessionManager.currentCastSession?.remoteMediaClient

    /** Control messages from the TV: persist watch time, or switch episodes app-scoped. */
    private fun onControl(msg: TvControlMessage) {
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
        val userAgent = customData.optString("userAgent")
            .ifBlank { dataStore.getPreferencesGroup(AdvancedPreferences).userAgent }
        val themeJson = if (customData.has("theme")) customData.optString("theme").ifBlank { null } else null
        val displayMode = if (anime.displayMode is Anime.DisplayMode.NAME) "NAME" else "NUMBER"
        val resume = if (fresh.seen) 0L else fresh.timeSeen
        val subtitlePrefs = playerPrefs.subtitlePrefLanguages.split(",").filter { it.isNotBlank() }
        val source = sourcesManager.getOrStub(anime.source)

        fetchDisposable?.dispose()
        fetchDisposable = source.fetchEpisodeSources(fresh.url).subscribe(
            { sources: List<StreamSource> ->
                val selected = sources.firstOrNull() ?: return@subscribe
                val request = buildCastLoadRequest(
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
                    resumeTimeMs = resume,
                )
                scope.launch { remoteMediaClient?.load(request) }
            },
            { /* fetch error: leave current playback as-is */ },
        )
    }

    private fun episodeFromCustomData(customData: JSONObject): Episode {
        val id = (customData.get("episodeId") as? Number)?.toLong() ?: 0L
        val seen = customData.optBoolean("seen", false)
        return Episode.create().copy(id = id, seen = seen)
    }

    private fun saveTime(episode: Episode, totalTime: Long, timeSeen: Long) {
        scope.launch {
            val threshold = dataStore.getPreferencesGroup(PlayerPreferences).seenThreshold
            if (episode.seen || episode.id == 0L) return@launch
            if (totalTime > 0L && timeSeen > 999L) {
                withContext(Dispatchers.IO) {
                    val watched = (timeSeen.toDouble() / totalTime) * 100 > threshold
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
