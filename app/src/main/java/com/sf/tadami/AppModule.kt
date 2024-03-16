package com.sf.tadami

import android.app.Application
import androidx.core.content.ContextCompat
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.sf.tadami.data.AndroidDatabaseHandler
import com.sf.tadami.data.DataBaseHandler
import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.data.anime.AnimeRepositoryImpl
import com.sf.tadami.data.dateColumnAdapter
import com.sf.tadami.data.download.DownloadProvider
import com.sf.tadami.data.download.TadamiDownloadManager
import com.sf.tadami.data.episode.EpisodeRepository
import com.sf.tadami.data.episode.EpisodeRepositoryImpl
import com.sf.tadami.data.history.HistoryRepository
import com.sf.tadami.data.history.HistoryRepositoryImpl
import com.sf.tadami.data.interactors.anime.AnimeWithEpisodesInteractor
import com.sf.tadami.data.interactors.anime.FetchIntervalInteractor
import com.sf.tadami.data.interactors.anime.UpdateAnimeInteractor
import com.sf.tadami.data.interactors.extension.GetExtensionLanguages
import com.sf.tadami.data.interactors.extension.GetExtensionsByType
import com.sf.tadami.data.interactors.history.GetHistoryInteractor
import com.sf.tadami.data.interactors.history.GetNextEpisodeInteractor
import com.sf.tadami.data.interactors.history.RemoveHistoryInteractor
import com.sf.tadami.data.interactors.history.UpdateHistoryInteractor
import com.sf.tadami.data.interactors.library.LibraryInteractor
import com.sf.tadami.data.interactors.sources.GetEnabledSources
import com.sf.tadami.data.interactors.sources.GetLanguagesWithSources
import com.sf.tadami.data.interactors.sources.GetSourcesWithNonLibraryAnime
import com.sf.tadami.data.interactors.updates.GetUpdatesInteractor
import com.sf.tadami.data.listOfStringsAdapter
import com.sf.tadami.data.sources.SourceRepository
import com.sf.tadami.data.sources.SourceRepositoryImpl
import com.sf.tadami.data.sources.StubSourceRepository
import com.sf.tadami.data.sources.StubSourceRepositoryImpl
import com.sf.tadami.data.updates.UpdatesRepository
import com.sf.tadami.data.updates.UpdatesRepositoryImpl
import com.sf.tadami.extension.ExtensionManager
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.network.player.PlayerNetworkHelper
import com.sf.tadami.ui.tabs.browse.SourceManager
import com.sf.tadami.ui.tabs.browse.SourceManagerImplementation
import data.Anime
import data.History
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

class AppModule(private val app: Application) : InjektModule {

    @OptIn(ExperimentalSerializationApi::class)
    override fun InjektRegistrar.registerInjectables() {

        addSingleton(app)

        // Scopes Handler

        addSingleton(DataStoresHandler())

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

        addSingletonFactory {
            Database(
                driver = get(),
                AnimeAdapter = Anime.Adapter(
                    genresAdapter = listOfStringsAdapter
                ),
                HistoryAdapter = History.Adapter(
                    seen_atAdapter = dateColumnAdapter
                )
            )
        }

        addSingletonFactory<DataBaseHandler> {
            AndroidDatabaseHandler(get())
        }

        // Extensions

        addSingletonFactory { ExtensionManager(app) }

        // Sources
        addSingletonFactory<StubSourceRepository>{
            StubSourceRepositoryImpl(get())
        }

        // Downloads

        addSingletonFactory {
            DownloadProvider(app)
        }

        addSingletonFactory {
            TadamiDownloadManager()
        }

        addSingletonFactory<SourceManager>{
            SourceManagerImplementation(app,get(),get())
        }

        // DataSources

        addSingletonFactory<AnimeRepository>{
            AnimeRepositoryImpl(get(),get())
        }

        addSingletonFactory<EpisodeRepository>{
            EpisodeRepositoryImpl(get())
        }

        addSingletonFactory<SourceRepository>{
            SourceRepositoryImpl(get(),get())
        }

        addSingletonFactory<HistoryRepository>{
            HistoryRepositoryImpl(get())
        }

        addSingletonFactory<UpdatesRepository>{
            UpdatesRepositoryImpl(get())
        }

        // Anime interactors

        addSingletonFactory {
            UpdateAnimeInteractor(get(),get(),get())
        }

        addSingletonFactory {
            AnimeWithEpisodesInteractor(get(),get())
        }

        addSingletonFactory {
            FetchIntervalInteractor(get())
        }

        addSingletonFactory {
            GetNextEpisodeInteractor(get(),get())
        }

        addSingletonFactory {
            RemoveHistoryInteractor(get())
        }


        // Library interactors

        addSingletonFactory {
            LibraryInteractor(get())
        }

        // Sources interactors

        addSingletonFactory {
            GetSourcesWithNonLibraryAnime(get())
        }

        addSingletonFactory {
            GetEnabledSources(get(),get())
        }

        addSingletonFactory {
            GetLanguagesWithSources(app,get(),get())
        }

        // History interactors

        addSingletonFactory {
            GetHistoryInteractor(get())
        }

        addSingletonFactory {
            UpdateHistoryInteractor(get())
        }

        addSingletonFactory {
            RemoveHistoryInteractor(get())
        }

        // Updates Interactors

        addSingletonFactory {
            GetUpdatesInteractor(get())
        }

        // Extensions Interactors

        addSingletonFactory {
            GetExtensionsByType(get(),get())
        }

        addSingletonFactory {
            GetExtensionLanguages(app,get(),get())
        }

        // HttpClient

        addSingletonFactory { NetworkHelper(app) }

        addSingletonFactory {
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }
        }

        // Player Network Client

        addSingletonFactory { PlayerNetworkHelper(app) }


        // Asynchronously init expensive components for a faster cold start
        ContextCompat.getMainExecutor(app).execute {
            get<NetworkHelper>()
            get<SourceManager>()
            get<Database>()
            get<PlayerNetworkHelper>()
        }
    }
}
