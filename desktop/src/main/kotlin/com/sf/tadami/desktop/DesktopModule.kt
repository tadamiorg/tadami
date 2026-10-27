package com.sf.tadami.desktop

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sf.tadami.BackgroundJobs
import com.sf.tadami.DataStoresHandler
import com.sf.tadami.Database
import com.sf.tadami.data.download.DownloadPlatform
import com.sf.tadami.data.providers.FoldersProvider
import com.sf.tadami.desktop.browser.EmbeddedBrowser
import com.sf.tadami.desktop.extension.DesktopExtensionPlatform
import com.sf.tadami.extension.util.ExtensionPlatform
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.network.player.VideoCache
import com.sf.tadami.registerSharedInjectables
import com.sf.tadami.storage.DesktopStorageFiles
import com.sf.tadami.storage.StorageFiles
import com.sf.tadami.ui.PlatformUi
import dev.mihon.injekt.patchInjekt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.addSingletonFactory
import java.io.File
import java.util.Properties

/** The singletons of the desktop app, as AppModule and PreferencesModule register Android's. */
class DesktopModule(private val platformUi: DesktopPlatformUi) : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addSingleton(DataStoresHandler { File(AppDirs.preferences, "$it.preferences_pb") })

        addSingletonFactory {
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler(produceNewData = { emptyPreferences() }),
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = { File(AppDirs.preferences, "$USER_PREFERENCES.preferences_pb") },
            )
        }

        addSingletonFactory<SqlDriver> {
            // Created, or migrated to the current version, as the Android driver does
            JdbcSqliteDriver(
                url = "jdbc:sqlite:" + File(AppDirs.data, "tadami.db").absolutePath,
                properties = Properties().apply { put("foreign_keys", "true") },
                schema = Database.Schema,
            ).apply {
                execute(null, "PRAGMA journal_mode = WAL", 0)
                execute(null, "PRAGMA synchronous = NORMAL", 0)
            }
        }

        registerSharedInjectables()

        addSingletonFactory<ExtensionPlatform> { DesktopExtensionPlatform() }
        addSingletonFactory<PlatformUi> { platformUi }
        addSingletonFactory<StorageFiles> { DesktopStorageFiles() }
        addSingletonFactory<DownloadPlatform> { DesktopDownloadPlatform() }
        addSingletonFactory<BackgroundJobs> { DesktopBackgroundJobs() }
        addSingletonFactory<FoldersProvider> { DesktopFoldersProvider() }

        addSingletonFactory { NetworkHelper(AppDirs.cache, EmbeddedBrowser) }

        // mpv caches the streams itself, in memory
        addSingletonFactory<VideoCache> {
            object : VideoCache {
                override val cacheSpace: Long = 0
                override fun clearCache() = Unit
            }
        }
    }

    companion object {
        private const val USER_PREFERENCES = "user_preferences"

        fun install(platformUi: DesktopPlatformUi) {
            // Builds each singleton once, even when several threads first ask for it at the same time
            patchInjekt()
            Injekt.importModule(DesktopModule(platformUi))
        }
    }
}
