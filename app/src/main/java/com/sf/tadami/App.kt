package com.sf.tadami


import android.app.ActivityManager
import android.app.Application
import android.content.Context
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.sf.tadami.network.interceptors.ImageLoaderInterceptor
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.preferences.appearance.AppearancePreferences
import com.sf.tadami.preferences.appearance.setAppCompatDelegateThemeMode
import com.sf.tadami.utils.animatorDurationScale
import com.sf.tadami.utils.getPreferencesGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

open class App : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        Injekt.importModule(AppModule(this))
        Injekt.importModule(PreferencesModule(this))
        createNotificationChannels()
        appContext = applicationContext

        val dataStore: DataStore<Preferences> = Injekt.get()
        val appearancePreferences: AppearancePreferences = runBlocking {
            dataStore.getPreferencesGroup(AppearancePreferences)
        }

        setAppCompatDelegateThemeMode(appearancePreferences.themeMode)
    }

    companion object {
        private var appContext: Context? = null
        fun getAppContext(): Context? {

            return appContext
        }
    }

    private fun createNotificationChannels(){
        Notifications.setupNotificationsChannels(this)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    // This header will be added to every image request.
                    .addInterceptor(ImageLoaderInterceptor())
                    .build()
            }
            .crossfade((300 * this@App.animatorDurationScale).toInt())
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .networkCachePolicy(CachePolicy.ENABLED)
            .allowRgb565(getSystemService<ActivityManager>()!!.isLowRamDevice)
            .fetcherDispatcher(Dispatchers.IO.limitedParallelism(8))
            .decoderDispatcher(Dispatchers.IO.limitedParallelism(2))
            .transformationDispatcher(Dispatchers.IO.limitedParallelism(2))
            .build()
    }

}


