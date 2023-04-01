package com.sf.tadami

import android.app.Application
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.sf.tadami.data.AndroidDatabaseHandler
import com.sf.tadami.data.DataBaseHandler
import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.data.anime.AnimeRepositoryImpl
import com.sf.tadami.data.episode.EpisodeRepository
import com.sf.tadami.data.episode.EpisodeRepositoryImpl
import com.sf.tadami.data.interactors.AnimeWithEpisodesInteractor
import com.sf.tadami.data.interactors.LibraryInteractor
import com.sf.tadami.data.interactors.UpdateAnimeInteractor
import com.sf.tadami.network.database.listOfStringsAdapter
import com.sf.tadami.network.requests.okhttp.HttpClient
import com.sf.tadami.ui.tabs.animesources.AnimeSourcesManager
import data.Anime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.api.*

class AppModule(private val app: Application) : InjektModule {

    @OptIn(ExperimentalSerializationApi::class)
    override fun InjektRegistrar.registerInjectables() {

        addSingleton(app)

        // Database

        addSingletonFactory<SqlDriver> {
            AndroidSqliteDriver(
                schema = Database.Schema,
                context = app,
                name = "tadami.db",
                callback = object : AndroidSqliteDriver.Callback(Database.Schema) {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        Log.i("Database","Opened")
                        super.onOpen(db)
                    }
                },
            )
        }

        addSingletonFactory {
            Database(
                driver = get(),
                AnimeAdapter = Anime.Adapter(
                    genresAdapter = listOfStringsAdapter,
                ),
            )
        }

        addSingletonFactory<DataBaseHandler> {
            AndroidDatabaseHandler(get())
        }

        // Sources

        addSingletonFactory{AnimeSourcesManager()}

        // DataSources

        addSingletonFactory<AnimeRepository>{
            AnimeRepositoryImpl(get(),get())
        }

        addSingletonFactory<EpisodeRepository>{
            EpisodeRepositoryImpl(get())
        }

        addSingletonFactory {
            UpdateAnimeInteractor(get(),get())
        }

        addSingletonFactory {
            AnimeWithEpisodesInteractor(get(),get())
        }

        addSingletonFactory {
            LibraryInteractor(get())
        }

        // HttpClient

        addSingletonFactory { HttpClient(app) }

        addSingletonFactory {
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }
        }

        // Asynchronously init expensive components for a faster cold start
        ContextCompat.getMainExecutor(app).execute {
            get<HttpClient>()
            get<Database>()
            get<DataBaseHandler>()
            get<AnimeRepository>()
            get<AnimeSourcesManager>()
        }
    }
}
