package com.sf.tadami

import com.sf.tadami.data.download.DownloadPlatform
import com.sf.tadami.data.download.AndroidDownloadPlatform
import com.sf.tadami.storage.StorageFiles
import com.sf.tadami.storage.AndroidStorageFiles
import com.sf.tadami.ui.PlatformUi
import com.sf.tadami.ui.AndroidPlatformUi
import androidx.datastore.preferences.preferencesDataStoreFile
import com.sf.tadami.extension.util.AndroidExtensionPlatform
import com.sf.tadami.extension.util.ExtensionPlatform
import android.app.Application
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.sf.tadami.data.download.TadamiDownloadManager
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.network.player.PlayerNetworkHelper
import com.sf.tadami.network.player.VideoCache
import com.sf.tadami.ui.tabs.browse.SourceManager
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import kotlinx.serialization.ExperimentalSerializationApi
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get
import kotlin.concurrent.thread

class AppModule(private val app: Application, private val warmUp: Boolean = true) : InjektModule {

    @OptIn(ExperimentalSerializationApi::class)
    override fun InjektRegistrar.registerInjectables() {

        addSingleton(app)

        // Scopes Handler

        addSingleton(DataStoresHandler { app.preferencesDataStoreFile(it) })

        // Database

        addSingletonFactory<SqlDriver> {
            AndroidSqliteDriver(
                schema = Database.Schema,
                context = app,
                name = "tadami.db",
                factory = RequerySQLiteOpenHelperFactory(),
                callback = object : AndroidSqliteDriver.Callback(Database.Schema) {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        setPragma(db, "foreign_keys = ON")
                        setPragma(db, "journal_mode = WAL")
                        setPragma(db, "synchronous = NORMAL")
                    }
                    private fun setPragma(db: SupportSQLiteDatabase, pragma: String) {
                        val cursor = db.query("PRAGMA $pragma")
                        cursor.moveToFirst()
                        cursor.close()
                    }
                },
            )
        }

        registerSharedInjectables()

        // Extensions

        addSingletonFactory<ExtensionPlatform> { AndroidExtensionPlatform(app) }

        addSingletonFactory<PlatformUi> { AndroidPlatformUi(app) }

        addSingletonFactory<StorageFiles> { AndroidStorageFiles(app) }

        addSingletonFactory<DownloadPlatform> { AndroidDownloadPlatform(app) }

        addSingletonFactory<BackgroundJobs> { AndroidBackgroundJobs(app) }

        // HttpClient

        addSingletonFactory { NetworkHelper(app) }

        // Player Network Client

        addSingletonFactory { PlayerNetworkHelper(app) }
        addSingletonFactory<VideoCache> { get<PlayerNetworkHelper>() }


        // Init expensive components off the main thread for a faster cold start. A caller asking
        // for one meanwhile waits for it, the singletons are thread safe (patchInjekt)
        if (warmUp) thread(name = "AppModuleWarmUp") {
            get<NetworkHelper>()
            get<SourceManager>()
            get<Database>()
            get<PlayerNetworkHelper>()
            // Restores the download queue and scans the downloads before the first screen asks
            get<TadamiDownloadManager>()
        }
    }
}
