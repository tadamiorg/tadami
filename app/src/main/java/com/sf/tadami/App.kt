package com.sf.tadami


import android.app.ActivityManager
import android.app.Application
import android.os.StrictMode
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.sf.tadami.crash.CrashActivity
import com.sf.tadami.crash.CrashLogBuffer
import com.sf.tadami.crash.GlobalExceptionHandler
import com.sf.tadami.network.interceptors.ImageLoaderInterceptor
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.preferences.appearance.AppearancePreferences
import com.sf.tadami.preferences.appearance.setAppCompatDelegateThemeMode
import com.sf.tadami.utils.animatorDurationScale
import com.sf.tadami.utils.getPreferencesGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger
import okhttp3.OkHttpClient
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

open class App : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        installLogging()
        if (BuildConfig.DEBUG) installStrictMode()

        GlobalExceptionHandler.initialize(applicationContext, CrashActivity::class.java)

        // Registration itself is cheap — every binding is a lazy factory — and the crash screen
        // still needs DI for the theme and toasts, so this runs in every process.
        Injekt.importModule(AppModule(this))
        Injekt.importModule(PreferencesModule(this))

        // CrashActivity lives in a ":error_handler" process, and Application.onCreate runs in
        // every process. Warming up there would re-open the database and re-instantiate every
        // extension — the very code most likely to have just crashed.
        if (!isMainProcess()) return

        createNotificationChannels()
        AppModule.warmUp()

        val dataStore: DataStore<Preferences> = Injekt.get()
        val appearancePreferences: AppearancePreferences = runBlocking {
            dataStore.getPreferencesGroup(AppearancePreferences)
        }

        setAppCompatDelegateThemeMode(appearancePreferences.themeMode)
    }

    /**
     * `logcat {}` is a no-op until a logger is installed, so until now every call through
     * [com.sf.tadami.utils.logcat] — including the crash handler's own — was silently discarded.
     *
     * This deliberately installs in release too: [AndroidLogcatLogger] writes via `Log.println`,
     * which ProGuard's `-assumenosideeffects` rule for `Log.d` does not strip, and
     * [CrashLogBuffer] is what gives crash reports their surrounding context.
     */
    private fun installLogging() {
        if (LogcatLogger.isInstalled) return
        LogcatLogger.install()
        LogcatLogger.loggers += AndroidLogcatLogger(
            minPriority = if (BuildConfig.DEBUG) LogPriority.DEBUG else LogPriority.WARN,
        )
        LogcatLogger.loggers += CrashLogBuffer
    }

    /**
     * Debug-only guard rail. Extension loading, the database and preference reads have all been
     * moved off the UI thread; this makes a regression show up as a log line instead of as a
     * dropped-frames report nobody notices.
     */
    private fun installStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build(),
        )
    }

    private fun isMainProcess(): Boolean {
        // Application.getProcessName() is the API 28 static (minSdk is already above that);
        // Context.getProcessName() only exists from API 33, so qualify it explicitly rather
        // than let overload resolution pick.
        val processName = Application.getProcessName()
        return processName == null || processName == packageName
    }
    private fun createNotificationChannels(){
        Notifications.setupNotificationsChannels(this)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    // Without these an unresponsive image host would hold a Coil fetcher slot
                    // indefinitely; the defaults are "no timeout".
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .callTimeout(2, TimeUnit.MINUTES)
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
