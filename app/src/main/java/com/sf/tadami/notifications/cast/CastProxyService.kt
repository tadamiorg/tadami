package com.sf.tadami.notifications.cast

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.ui.animeinfos.episode.cast.ProxyServer

class CastProxyService : Service() {

    private val castProxyServer = ProxyServer()
    private val castListener :  SessionManagerListener<CastSession> = getCastListener()
    private lateinit var notifier : CastNotifier
    private lateinit var castContext : CastContext

    override fun onCreate() {
        super.onCreate()
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

    private fun getCastListener() : SessionManagerListener<CastSession>{
        return object : SessionManagerListener<CastSession> {
            override fun onSessionEnded(session: CastSession, error: Int) {
                onApplicationDisconnected(session)
            }

            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {

            }

            override fun onSessionResumeFailed(session: CastSession, error: Int) {
                onApplicationDisconnected(session)
            }

            override fun onSessionStarted(session: CastSession, sessionId: String) {

            }

            override fun onSessionStartFailed(session: CastSession, error: Int) {
                onApplicationDisconnected(session)
            }

            override fun onSessionStarting(session: CastSession) {}
            override fun onSessionEnding(session: CastSession) {}
            override fun onSessionResuming(session: CastSession, sessionId: String) {}
            override fun onSessionSuspended(session: CastSession, reason: Int) {}
            private fun onApplicationDisconnected(session: CastSession) {
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