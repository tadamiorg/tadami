package com.sf.tadami

import android.app.Application
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.sf.tadami.data.providers.AndroidFoldersProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingletonFactory

private const val USER_PREFERENCES = "user_preferences"

class PreferencesModule(private val app: Application) : InjektModule {
    override fun InjektRegistrar.registerInjectables() {

        // DataStore
        addSingletonFactory {
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler(
                    produceNewData = { emptyPreferences() }
                ),
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = { app.preferencesDataStoreFile(USER_PREFERENCES) }
            )
        }

        // Data storage
        addSingletonFactory {
            AndroidFoldersProvider(app)
        }
    }
}