package com.sf.animescraper


import android.app.Application
import android.content.Context
import com.sf.animescraper.AppModule
import uy.kohesive.injekt.Injekt


open class App : Application() {

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

}
