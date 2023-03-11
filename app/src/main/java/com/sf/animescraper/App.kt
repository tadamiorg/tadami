package com.sf.animescraper


import android.app.ActivityManager
import android.app.Application
import android.content.Context
import androidx.core.content.getSystemService
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.sf.animescraper.utils.animatorDurationScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import uy.kohesive.injekt.Injekt


open class App : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        Injekt.importModule(AppModule(this))
        appContext = applicationContext

    }

    companion object {
        private var appContext: Context? = null
        fun getAppContext(): Context? {
            return appContext
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
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


