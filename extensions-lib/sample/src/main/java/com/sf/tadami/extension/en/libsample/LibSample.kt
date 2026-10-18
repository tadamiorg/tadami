package com.sf.tadami.extension.en.libsample

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sf.tadami.network.GET
import com.sf.tadami.network.awaitSuccess
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier
import com.sf.tadami.preferences.model.SourcePreference
import com.sf.tadami.source.AnimesPage
import com.sf.tadami.source.model.AnimeFilterList
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.model.SEpisode
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.source.online.ConfigurableAnimeHttpSource
import com.sf.tadami.ui.tabs.browse.tabs.sources.preferences.SourcesPreferencesContent
import com.sf.tadami.utils.Lang
import okhttp3.Request
import okhttp3.Response

/**
 * A source of lib 1.7: only the suspend methods, with its site's address in its preferences. The site
 * answers one "<title>|<url>" line per anime, one URL per line for the episodes and the streams.
 */
class LibSample : ConfigurableAnimeHttpSource<LibSamplePreferences>(BuildConfig.SOURCE_ID, LibSamplePreferences) {

    override val name: String = "Lib sample"

    override val lang: Lang = Lang.ENGLISH

    override val baseUrl: String
        get() = preferences.baseUrl

    override suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList, noToasts: Boolean): AnimesPage =
        animesPage(get("/search?q=$query&page=$page"))

    override suspend fun getLatestAnime(page: Int): AnimesPage = animesPage(get("/latest?page=$page"))

    override suspend fun getAnimeDetails(anime: SAnime): SAnime = SAnime.create().apply {
        url = anime.url
        title = anime.title
        description = get(anime.url)
        initialized = true
    }

    override suspend fun getEpisodeList(anime: SAnime): List<SEpisode> =
        lines(get("${anime.url}/episodes")).mapIndexed { index, url ->
            SEpisode.create().apply {
                this.url = url
                name = "Episode ${index + 1}"
                episodeNumber = index + 1f
            }
        }

    override suspend fun getEpisodeSources(episode: SEpisode): List<StreamSource> =
        lines(get(episode.url)).map { StreamSource(url = it, fullName = "Sample", headers = headers) }

    override fun getPreferenceScreen() = SourcesPreferencesContent(
        title = name,
        preferences = listOf(
            SourcePreference.PreferenceCategory(
                title = "Site",
                preferenceItems = listOf(
                    SourcePreference.PreferenceItem.EditTextPreference(
                        value = preferences.baseUrl,
                        key = LibSamplePreferences.BASE_URL,
                        defaultValue = LibSamplePreferences.DEFAULT_BASE_URL,
                        title = "Address",
                    ),
                ),
            ),
        ),
    )

    private suspend fun get(path: String): String =
        client.newCall(GET(baseUrl + path, headers)).awaitSuccess().use { it.body.string() }

    private fun lines(body: String) = body.lines().map { it.trim() }.filter { it.isNotEmpty() }

    private fun animesPage(body: String) = AnimesPage(
        animes = lines(body).map { line ->
            SAnime.create().apply {
                title = line.substringBefore('|')
                url = line.substringAfter('|')
            }
        },
        hasNextPage = false,
    )

    // The Rx API of AnimeHttpSource, which the suspend methods above replace

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList, noToasts: Boolean): Request = notUsed()
    override fun searchAnimeParse(response: Response): AnimesPage = notUsed()
    override fun latestAnimesRequest(page: Int): Request = notUsed()
    override fun latestAnimeParse(response: Response): AnimesPage = notUsed()
    override fun animeDetailsParse(response: Response): SAnime = notUsed()
    override fun episodesListParse(response: Response): List<SEpisode> = notUsed()
    override fun episodeSourcesParse(response: Response): List<StreamSource> = notUsed()

    private fun notUsed(): Nothing = throw UnsupportedOperationException("Not used: the suspend methods are")
}

data class LibSamplePreferences(val baseUrl: String) : CustomPreferencesIdentifier {
    companion object : CustomPreferences<LibSamplePreferences> {
        val BASE_URL = stringPreferencesKey("base_url")
        const val DEFAULT_BASE_URL = "http://127.0.0.1:9"

        override fun transform(preferences: Preferences) = LibSamplePreferences(preferences[BASE_URL] ?: DEFAULT_BASE_URL)

        override fun setPrefs(newValue: LibSamplePreferences, preferences: MutablePreferences) {
            preferences[BASE_URL] = newValue.baseUrl
        }
    }
}
