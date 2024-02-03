package com.sf.tadami.ui.animeinfos.episode.cast

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.android.gms.cast.LaunchOptions
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.MediaIntentReceiver
import com.google.android.gms.cast.framework.media.NotificationOptions
import com.sf.tadami.R
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.ui.main.MainActivity
import com.sf.tadami.utils.getPreferencesGroup
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CastOptionsProvider : OptionsProvider {
    private val dataStore: DataStore<Preferences> = Injekt.get()
    private var playerPreferences : PlayerPreferences = runBlocking {
        dataStore.getPreferencesGroup(PlayerPreferences)
    }

    override fun getCastOptions(context: Context): CastOptions {
        val buttonActions: MutableList<String> = ArrayList()
        buttonActions.add(MediaIntentReceiver.ACTION_REWIND)
        buttonActions.add(MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK)
        buttonActions.add(MediaIntentReceiver.ACTION_FORWARD)
        buttonActions.add(MediaIntentReceiver.ACTION_STOP_CASTING)

        // Builds a notification with the above actions. Each tap on the "rewind" and "forward" buttons skips 30 seconds.

        val notificationOptions = NotificationOptions.Builder()
            .setTargetActivityClassName(MainActivity::class.java.name)
            .setActions(buttonActions, intArrayOf(0,1,2,3))
            .setSkipStepMs(playerPreferences.doubleTapLength)
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