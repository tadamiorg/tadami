package com.sf.tadami.ui.animeinfos.episode.cast

import android.content.Context
import android.text.format.DateUtils
import com.google.android.gms.cast.LaunchOptions
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.MediaIntentReceiver
import com.google.android.gms.cast.framework.media.NotificationOptions
import com.google.android.gms.cast.framework.media.widget.ExpandedControllerActivity
import com.sf.tadami.ui.animeinfos.episode.EpisodeActivity
import com.sf.tadami.ui.main.MainActivity
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**
 * Implements [OptionsProvider] to provide [CastOptions].
 */
class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        val notificationOptions = NotificationOptions.Builder()
            .setTargetActivityClassName(MainActivity::class.java.name)
            .build()
        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .build()
        val launchOptions = LaunchOptions.Builder()
            .setAndroidReceiverCompatible(false)
            .build()
        return CastOptions.Builder()
            .setLaunchOptions(launchOptions)
            .setReceiverApplicationId("DA2F4B1A")
            .setCastMediaOptions(mediaOptions)
            .build()
    }

    override fun getAdditionalSessionProviders(appContext: Context): List<SessionProvider>? {
        return null
    }
}