package com.sf.tadami.notifications.cast

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.sf.tadami.data.interactors.UpdateAnimeInteractor
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.ui.animeinfos.episode.cast.ProxyServer
import com.sf.tadami.ui.tabs.settings.screens.player.PlayerPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CastProxyService : Service() {

    private val castProxyServer = ProxyServer()
    private val castListener :  SessionManagerListener<CastSession> = getCastListener()
    private lateinit var notifier : CastNotifier
    private lateinit var castContext : CastContext
    private val updateAnimeInteractor: UpdateAnimeInteractor = Injekt.get()
    private val dataStore: DataStore<Preferences> = Injekt.get()
    private lateinit var playerPreferences : PlayerPreferences

    override fun onCreate() {
        super.onCreate()
        playerPreferences = runBlocking {
            dataStore.data.map { preferences ->
                PlayerPreferences.transform(preferences)
            }.first()
        }
        notifier = CastNotifier(applicationContext)
        castContext = CastContext.getSharedInstance(applicationContext)
        castContext.sessionManager.addSessionManagerListener(
            castListener,
            CastSession::class.java
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        setForegroundService()
        startProxyServer()
        return START_STICKY
    }

    private fun setForegroundService() {
        startForeground(Notifications.CAST_PROXY_STATUS_NOTIFICATION, notifier.castStatusNotificationBuilder.build())
    }

    override fun onDestroy() {
        stopProxyServer()
        castContext.sessionManager.removeSessionManagerListener(
            castListener,
            CastSession::class.java
        )
        super.onDestroy()
    }

    private fun startProxyServer() {
        castProxyServer.start()
    }

    private fun stopProxyServer() {
        castProxyServer.stop()
    }

    override fun onBind(intent: Intent?): IBinder? {
        // If your service does not support binding, return null
        return null
    }

    fun updateTime(episode: Episode?, totalTime: Long, timeSeen: Long, threshold: Int) : Job? {
        episode?.let { ep ->
            if (ep.seen) return null
            return CoroutineScope(Dispatchers.IO).launch {
                if (totalTime > 0L && timeSeen > 999L) {
                    val watched = (timeSeen.toDouble() / totalTime) * 100 > threshold
                    if (watched) {
                        updateAnimeInteractor.awaitSeenEpisodeUpdate(setOf(ep.id), true)
                    } else {
                        updateAnimeInteractor.awaitSeenEpisodeTimeUpdate(ep, totalTime, timeSeen)
                    }
                }
            }
        }
        return null
    }

    private fun getCastListener() : SessionManagerListener<CastSession>{
        return object : SessionManagerListener<CastSession> {
            override fun onSessionEnded(session: CastSession, error: Int) {
                onApplicationDisconnected()
            }

            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {

            }

            override fun onSessionResumeFailed(session: CastSession, error: Int) {
                onApplicationDisconnected()
            }

            override fun onSessionStarted(session: CastSession, sessionId: String) {

            }

            override fun onSessionStartFailed(session: CastSession, error: Int) {
                onApplicationDisconnected()
            }

            override fun onSessionStarting(session: CastSession) {}
            override fun onSessionEnding(session: CastSession) {
                val mediaClient = session.remoteMediaClient
                if(mediaClient != null && mediaClient.mediaInfo !=null && mediaClient.mediaInfo!!.customData != null){
                    val episodeId = mediaClient.mediaInfo!!.customData!!.getLong("episodeId")
                    val episodeSeen = mediaClient.mediaInfo!!.customData!!.getBoolean("seen")
                    val totalTime = mediaClient.streamDuration
                    val timeSeen = mediaClient.approximateStreamPosition
                    updateTime(episode = Episode.create().copy(id = episodeId, seen = episodeSeen),totalTime = totalTime, timeSeen = timeSeen, threshold = playerPreferences.seenThreshold)
                }
            }
            override fun onSessionResuming(session: CastSession, sessionId: String) {}
            override fun onSessionSuspended(session: CastSession, reason: Int) {}
            private fun onApplicationDisconnected() {
                stop(this@CastProxyService)
            }
        }
    }

    companion object{
        fun startNow(context: Context){
            val intent = Intent(context, CastProxyService::class.java)
            context.startService(intent)
        }
        fun stop(context: Context){
            val serviceIntent = Intent(context, CastProxyService::class.java)
            context.stopService(serviceIntent)
        }
    }
}