package com.sf.animescraper

import android.app.Application
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.sf.animescraper.network.database.booleanAdapter
import com.sf.animescraper.network.database.listOfStringsAdapter
import com.sf.animescraper.network.requests.okhttp.HttpClient
import com.sf.animescraper.ui.shared.SharedViewModel
import com.sf.animescraper.ui.tabs.animesources.AnimeSourcesManager
import data.Anime
import data.Episode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.api.*

class AppModule(private val app: Application) : InjektModule {

    @OptIn(ExperimentalSerializationApi::class)
    override fun InjektRegistrar.registerInjectables() {

        addSingleton(app)

        // Database

        val sqlDriverAnime = AndroidSqliteDriver(
            schema = Database.Schema,
            context = app,
            name = "animescraper.db",
            callback = object : AndroidSqliteDriver.Callback(Database.Schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    Log.i("Database","Opened")
                    super.onOpen(db)
                }
            },
        )

        addSingletonFactory {
            Database(
                driver = sqlDriverAnime,
                AnimeAdapter = Anime.Adapter(
                    genreAdapter = listOfStringsAdapter,

                ),
            )
        }

        // HttpClient

        addSingletonFactory { HttpClient(app) }

        // SharedViewModels

        addSingletonFactory{SharedViewModel()}

        // Sources

        addSingletonFactory{AnimeSourcesManager()}

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

            get<AnimeSourcesManager>()
        }
    }
}
