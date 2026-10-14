package com.sf.tadami


import android.app.ActivityManager
import android.app.Application
import android.webkit.WebView
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.sf.tadami.crash.CrashActivity
import com.sf.tadami.crash.GlobalExceptionHandler
import com.sf.tadami.network.HttpException
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.network.interceptors.ImageLoaderInterceptor
import com.sf.tadami.network.utils.WebViewUtil
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.preferences.appearance.AppearancePreferences
import com.sf.tadami.preferences.appearance.setAppCompatDelegateThemeMode
import com.sf.tadami.utils.animatorDurationScale
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.logcat
import io.reactivex.rxjava3.exceptions.UndeliverableException
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.IOException

open class App : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        installLogging()
        setupRxJavaErrorHandler()

        // The crash screen runs in its own process, which runs this too
        val isMainProcess = getProcessName() == packageName
        if (isMainProcess) {
            GlobalExceptionHandler.initialize(applicationContext, CrashActivity::class.java)
            // Before the network layer builds the default user agent from it
            WebViewUtil.detectVersion(this)
        } else {
            // A crash in the crash screen process must not relaunch the crash screen forever,
            // and WebView refuses to share its data directory between processes
            WebView.setDataDirectorySuffix(getProcessName().substringAfterLast(':'))
        }

        Injekt.importModule(AppModule(this, warmUp = isMainProcess))
        Injekt.importModule(PreferencesModule(this))
        createNotificationChannels()

        val dataStore: DataStore<Preferences> = Injekt.get()
        val appearancePreferences: AppearancePreferences = runBlocking {
            dataStore.getPreferencesGroup(AppearancePreferences)
        }

        setAppCompatDelegateThemeMode(appearancePreferences.themeMode)
    }
    /**
     * `logcat {}` is a no-op until a logger is installed. Installed in release too, at WARN and
     * above: [AndroidLogcatLogger] writes through `Log.println`, which ProGuard doesn't strip.
     */
    private fun installLogging() {
        if (LogcatLogger.isInstalled) return
        LogcatLogger.install()
        LogcatLogger.loggers += AndroidLogcatLogger(
            minPriority = if (BuildConfig.DEBUG) LogPriority.DEBUG else LogPriority.WARN,
        )
    }

    private fun setupRxJavaErrorHandler() {
        RxJavaPlugins.setErrorHandler { error ->
            // An error nobody is subscribed to anymore, e.g. a request failing after its screen
            // closed: not worth a crash
            if (error is UndeliverableException || error is IOException ||
                error is InterruptedException || error is HttpException
            ) {
                logcat(LogPriority.WARN, error) { "Undeliverable RxJava error" }
                return@setErrorHandler
            }
            // Likely a bug, crash as RxJava does without a handler
            val thread = Thread.currentThread()
            thread.uncaughtExceptionHandler?.uncaughtException(thread, error)
        }
    }

    private fun createNotificationChannels(){
        Notifications.setupNotificationsChannels(this)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient {
                // Shares the cookies (Cloudflare clearance) and user agent of the sources, without
                // ever opening a WebView for an image
                Injekt.get<NetworkHelper>().nonCloudflareClient.newBuilder()
                    // Coil has its own disk cache
                    .cache(null)
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
