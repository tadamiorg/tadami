package com.sf.tadami.data.update

import com.sf.tadami.BuildConfig
import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.requests.okhttp.HttpClient
import com.sf.tadami.network.requests.okhttp.asObservableSuccess
import com.sf.tadami.network.requests.okhttp.parseAs
import com.sf.tadami.ui.utils.awaitSingleOrError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AppUpdater {
    private val httpClient : HttpClient = Injekt.get()

    suspend fun checkForUpdate() : AppUpdate{
        return withContext(Dispatchers.IO) {
            val response = httpClient.client
                .newCall(GET("https://api.github.com/repos/$GITHUB_REPO/releases/latest"))
                .asObservableSuccess()
                .awaitSingleOrError() ?: return@withContext AppUpdate.NoNewUpdate
            try{
                response.parseAs<GithubUpdate>().let{
                    val isNewversion = checkNewVersion(it.version)
                    if(isNewversion) AppUpdate.NewUpdate(it)
                    else AppUpdate.NoNewUpdate
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
        const val GITHUB_REPO = "AnimeScraper/Tadami"
    }
}