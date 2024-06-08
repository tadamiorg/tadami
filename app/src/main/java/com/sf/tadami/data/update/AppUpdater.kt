package com.sf.tadami.data.update

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.BuildConfig
import com.sf.tadami.network.GET
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.network.asObservableSuccess
import com.sf.tadami.network.parseAs
import com.sf.tadami.preferences.app.UpdatePreferences
import com.sf.tadami.ui.utils.awaitSingleOrNull
import com.sf.tadami.utils.editPreference
import com.sf.tadami.utils.getPreferencesGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.temporal.ChronoUnit

class AppUpdater {
    private val networkHelper : NetworkHelper = Injekt.get()
    private val json : Json = Injekt.get()
    private val dataStore : DataStore<Preferences> = Injekt.get()

    suspend fun checkForUpdate(forceCheck : Boolean = false) : AppUpdate{
        val now = Instant.now()

        // Limit checks to once every 3 days at most
        if (!forceCheck && now.isBefore(
                Instant.ofEpochMilli(dataStore.getPreferencesGroup(UpdatePreferences).lastUpdatedCheckTimestamp).plus(3, ChronoUnit.DAYS),
            )
        ) {
            return AppUpdate.NoNewUpdate
        }

        dataStore.editPreference(now.toEpochMilli(),UpdatePreferences.APP_UPDATE_CHECK_LAST_TIMESTAMP)

        return withContext(Dispatchers.IO) {
            val response = networkHelper.client
                .newCall(GET("https://api.github.com/repos/$GITHUB_REPO/releases/latest"))
                .asObservableSuccess()
                .awaitSingleOrNull(printErrors = false) ?: return@withContext AppUpdate.NoNewUpdate
            try{
                with(json){
                    response.parseAs<GithubUpdate>().let{
                        val isNewversion = checkNewVersion(it.version)
                        if(isNewversion) AppUpdate.NewUpdate(it)
                        else AppUpdate.NoNewUpdate
                    }
                }

            } catch(e : Exception) {
                AppUpdate.NoNewUpdate
            }

        }
    }

    private fun checkNewVersion(newVersionTag : String) : Boolean{
        val newVersion = newVersionTag.replace("[^\\d.]".toRegex(), "").split(".").map { it.toInt() }
        val oldVersion = BuildConfig.VERSION_NAME.replace("[^\\d.]".toRegex(), "").split(".").map { it.toInt() }

        val versionComparison = oldVersion.indices.any { fragmentIndex ->
            if(newVersion[fragmentIndex] != oldVersion[fragmentIndex]){
                return newVersion[fragmentIndex] > oldVersion[fragmentIndex]
            }
            false
        }

        return versionComparison
    }

    companion object{
        const val GITHUB_REPO = "tadamiorg/tadami"

        val RELEASE_TAG: String by lazy {
            "v${BuildConfig.VERSION_NAME}"
        }

        val RELEASE_URL = "https://github.com/$GITHUB_REPO/releases/tag/$RELEASE_TAG"
    }
}


