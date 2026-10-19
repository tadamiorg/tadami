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
import com.sf.tadami.crash.CrashLogBuffer
import com.sf.tadami.crash.GlobalExceptionHandler
import com.sf.tadami.network.HttpException
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.network.interceptors.ImageLoaderInterceptor
import com.sf.tadami.network.utils.WebViewUtil
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.preferences.appearance.AppearancePreferences
import com.sf.tadami.preferences.appearance.setAppCompatDelegateThemeMode
import com.sf.tadami.ui.webview.adblock.AdBlockUpdateWorker
import com.sf.tadami.utils.animatorDurationScale
import com.sf.tadami.utils.coil.AnimeCoverFetcherFactory
import com.sf.tadami.utils.coil.AnimeCoverKeyer
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.logcat
import dev.mihon.injekt.patchInjekt
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
import kotlin.concurrent.thread

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

        // Builds each singleton once, even when several threads first ask for it at the same time:
        // the warm up of AppModule runs on its own thread
        patchInjekt()
        Injekt.importModule(AppModule(this, warmUp = isMainProcess))
        Injekt.importModule(PreferencesModule(this))
        createNotificationChannels()

        if (isMainProcess) {
            // The WebView ad blocker blocks nothing until its lists are downloaded
            thread(name = "AdBlockStartup") {
                try {
                    AdBlockUpdateWorker.downloadIfNeeded(this)
                } catch (e: Exception) {
                    logcat(LogPriority.WARN, e) { "Could not check the ad block lists" }
                }
            }
        }

        val dataStore: DataStore<Preferences> = Injekt.get()
        val appearancePreferences: AppearancePreferences = runBlocking {
            dataStore.getPreferencesGroup(AppearancePreferences)
        }

        setAppCompatDelegateThemeMode(appearancePreferences.themeMode)
    }
    /**
     * `logcat {}` is a no-op until a logger is installed. Installed in release too, at WARN and
     * above: [AndroidLogcatLogger] writes through `Log.println`, which ProGuard doesn't strip.
     * [CrashLogBuffer] keeps the last lines for the crash report.
     */
    private fun installLogging() {
        if (LogcatLogger.isInstalled) return
        LogcatLogger.install()
        LogcatLogger.loggers += AndroidLogcatLogger(
            minPriority = if (BuildConfig.DEBUG) LogPriority.DEBUG else LogPriority.WARN,
        )
        LogcatLogger.loggers += CrashLogBuffer
    }

    private fun setupRxJavaErrorHandler() {
        RxJavaPlugins.setErrorHandler { error ->
            // An error nobody is subscribed to anymore, e.g. a request failing after its screen
            // closed: not worth a crash. Unless it's a fatal error, a LinkageError of an extension
            // for instance: the uncaught exception handler disables the extension it comes from
            if (error is UndeliverableException && error.cause !is Error || error is IOException ||
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
            .components {
                // The covers of the database, keyed by when they were last refreshed
                add(AnimeCoverKeyer())
                add(AnimeCoverFetcherFactory())
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
