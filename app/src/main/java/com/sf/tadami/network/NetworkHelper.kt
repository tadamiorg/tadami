package com.sf.tadami.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.network.interceptors.CloudflareInterceptor
import com.sf.tadami.network.interceptors.IgnoreGzipInterceptor
import com.sf.tadami.network.interceptors.UncaughtExceptionInterceptor
import com.sf.tadami.network.interceptors.UserAgentInterceptor
import com.sf.tadami.preferences.advanced.AdvancedPreferences
import com.sf.tadami.utils.getPreferencesGroupAsFlow
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.concurrent.TimeUnit

class NetworkHelper(private val context : Context) {

    private val cacheDir = File(context.cacheDir, "network_cache")

    private val cacheSize = 5L * 1024 * 1024 // 5 MiB

    val cookieManager = AndroidCookieJar()

    private val dataStore : DataStore<Preferences> = Injekt.get()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Starts at the built-in defaults and is replaced as soon as DataStore emits, so constructing
     * this helper never blocks the thread that asks for it. Interceptors read it through a lambda
     * rather than capturing a value, which also means a User-Agent change in Advanced settings now
     * takes effect immediately instead of after a restart.
     */
    @Volatile
    var advancedPreferences: AdvancedPreferences = AdvancedPreferences.transform(emptyPreferences())
        private set

    init {
        scope.launch {
            dataStore.getPreferencesGroupAsFlow(AdvancedPreferences).collectLatest {
                advancedPreferences = it
            }
        }
    }

    private fun userAgent() = advancedPreferences.userAgent.trim()

    private val clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
        .cookieJar(cookieManager)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(2, TimeUnit.MINUTES)
        .cache(
            Cache(
                directory = cacheDir,
                maxSize = cacheSize, // 5 MiB
            ),
        )
        .addInterceptor(UncaughtExceptionInterceptor())
        .addInterceptor(UserAgentInterceptor(::userAgent))
        .addNetworkInterceptor(IgnoreGzipInterceptor())
        .addNetworkInterceptor(BrotliInterceptor)

    val nonCloudflareClient = clientBuilder.build()

    val client = clientBuilder
        .addInterceptor(
            CloudflareInterceptor(context, cookieManager, ::userAgent),
        )
        .build()

    /**
     * @deprecated Since Tadami 1.5.0
     */
    @Deprecated("The regular client handles Cloudflare by default")
    @Suppress("UNUSED")
    val cloudflareClient: OkHttpClient = client

    companion object{
        const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"
    }
}