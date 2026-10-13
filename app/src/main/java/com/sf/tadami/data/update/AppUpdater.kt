package com.sf.tadami.data.update

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.BuildConfig
import com.sf.tadami.network.GET
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.network.asObservableSuccess
import com.sf.tadami.network.parseAs
import com.sf.tadami.preferences.app.UpdatePreferences
import com.sf.tadami.ui.utils.awaitSingleOrError
import com.sf.tadami.utils.editPreference
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.logcat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.temporal.ChronoUnit

class AppUpdater {
    private val networkHelper : NetworkHelper = Injekt.get()
    private val json : Json = Injekt.get()
    private val dataStore : DataStore<Preferences> = Injekt.get()

    suspend fun checkForUpdate(forceCheck : Boolean = false) : AppUpdate{
        // Disabled in debug builds: the release APK has another applicationId and would install next to them
        if (!BuildConfig.UPDATER_ENABLED) {
            return AppUpdate.NoNewUpdate
        }

        val now = Instant.now()

        // Limit checks to once every 3 days at most
        if (!forceCheck && now.isBefore(
                Instant.ofEpochMilli(dataStore.getPreferencesGroup(UpdatePreferences).lastUpdatedCheckTimestamp).plus(3, ChronoUnit.DAYS),
            )
        ) {
            return AppUpdate.NoNewUpdate
        }

        return withContext(Dispatchers.IO) {
            try{
                val response = networkHelper.client
                    .newCall(GET("https://api.github.com/repos/$GITHUB_REPO/releases/latest"))
                    .asObservableSuccess()
                    .awaitSingleOrError()
                val release = with(json){
                    response.parseAs<GithubUpdate>()
                }
                val isNewVersion = checkNewVersion(release.version)

                // Newer release whose APK is not uploaded yet: offer nothing and check again on next launch
                if (isNewVersion && release.getDownloadLink() == null) {
                    return@withContext AppUpdate.NoNewUpdate
                }

                // Only a successful check starts the 3 days delay, so an offline launch does not skip it
                dataStore.editPreference(now.toEpochMilli(),UpdatePreferences.APP_UPDATE_CHECK_LAST_TIMESTAMP)

                if(isNewVersion) AppUpdate.NewUpdate(release)
                else AppUpdate.NoNewUpdate
            } catch(e : CancellationException) {
                throw e
            } catch(e : Exception) {
                this@AppUpdater.logcat(LogPriority.WARN, e) { "Could not read the latest release" }
                AppUpdate.CheckFailed
            }

        }
    }

    private fun checkNewVersion(newVersionTag : String) : Boolean{
        return isNewerVersion(newVersionTag, BuildConfig.RELEASE_VERSION_NAME)
    }

    companion object{
        const val GITHUB_REPO = "tadamiorg/tadami"

        val RELEASE_TAG: String by lazy {
            "v${BuildConfig.RELEASE_VERSION_NAME}"
        }

        val RELEASE_URL = "https://github.com/$GITHUB_REPO/releases/tag/$RELEASE_TAG"
    }
}

/**
 * Whether the release tagged [newTag] (`v1.10.0`) is newer than [currentVersion]. Missing segments
 * count as 0, so v1.9.3.1 is newer than 1.9.3 and v1.10 equals 1.10.0, and a pre-release
 * (`2.0.0-beta1`) is older than the release with the same numbers.
 */
internal fun isNewerVersion(newTag: String, currentVersion: String): Boolean {
    val newVersion = newTag.versionCore()
    val oldVersion = currentVersion.versionCore()
    for (fragmentIndex in 0 until maxOf(newVersion.size, oldVersion.size)) {
        val newFragment = newVersion.getOrElse(fragmentIndex) { 0 }
        val oldFragment = oldVersion.getOrElse(fragmentIndex) { 0 }
        if (newFragment != oldFragment) {
            return newFragment > oldFragment
        }
    }
    return currentVersion.isPreRelease() && !newTag.isPreRelease()
}

private fun String.versionCore(): List<Int> {
    return withoutVersionPrefix().substringBefore('-').substringBefore('+')
        .split(".").map { fragment -> fragment.filter(Char::isDigit).toIntOrNull() ?: 0 }
}

private fun String.isPreRelease(): Boolean = '-' in withoutVersionPrefix().substringBefore('+')

private fun String.withoutVersionPrefix(): String = trim().removePrefix("v")
