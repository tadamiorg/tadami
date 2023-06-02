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
import com.sf.tadami.R
import com.sf.tadami.ui.main.MainActivity

class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        val buttonActions: MutableList<String> = ArrayList()
        buttonActions.add(MediaIntentReceiver.ACTION_REWIND)
        buttonActions.add(MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK)
        buttonActions.add(MediaIntentReceiver.ACTION_FORWARD)
        buttonActions.add(MediaIntentReceiver.ACTION_STOP_CASTING)

        // Showing "play/pause" and "stop casting" in the compat view of the notification.
        val compatButtonActionsIndices = intArrayOf(1, 3)

        // Builds a notification with the above actions. Each tap on the "rewind" and "forward" buttons skips 30 seconds.

        val notificationOptions = NotificationOptions.Builder()
            .setTargetActivityClassName(MainActivity::class.java.name)
            .setActions(buttonActions, compatButtonActionsIndices)
            .setSkipStepMs(30 * DateUtils.SECOND_IN_MILLIS)
            .build()
        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .build()
        val launchOptions = LaunchOptions.Builder()
            .setAndroidReceiverCompatible(false)
            .build()
        return CastOptions.Builder()
            .setLaunchOptions(launchOptions)
            .setReceiverApplicationId(context.getString(R.string.cast_receiver_id))
            .setCastMediaOptions(mediaOptions)
            .build()
    }

    override fun getAdditionalSessionProviders(appContext: Context): List<SessionProvider>? {
        return null
    }
}