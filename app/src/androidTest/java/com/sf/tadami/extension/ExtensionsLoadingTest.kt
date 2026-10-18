package com.sf.tadami.extension

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.model.ExtensionLoadIssue
import com.sf.tadami.extension.model.LibVersion
import com.sf.tadami.extension.model.LoadResult
import com.sf.tadami.extension.util.ExtensionsLoader
import com.sf.tadami.extension.util.ExtensionsStorage
import com.sf.tadami.network.HttpException
import com.sf.tadami.preferences.extensions.TrustedExtensionsPreferences
import com.sf.tadami.source.AnimeCatalogueSource
import com.sf.tadami.source.ConfigurableSource
import com.sf.tadami.source.Source
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.online.AnimeHttpSource
import com.sf.tadami.source.online.ConfigurableAnimeHttpSource
import com.sf.tadami.source.online.ConfigurableParsedHttpAnimeSource
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Loads extensions from [ExtensionsStorage] the way the app does: those of the published repo, built
 * against lib 1.5 and 1.6, and lib-sample.apk (:extensions-lib:sample), built against the current
 * lib, with only the suspend methods.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ExtensionsLoadingTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val testContext: Context = InstrumentationRegistry.getInstrumentation().context
    private val dataStore: DataStore<Preferences> = Injekt.get()
    private val storageDir = ExtensionsStorage.dir(context)

    /** The packages this test put in the storage, with the APK each replaced if any, restored after it */
    private val copied = mutableMapOf<String, File?>()
    private val trusted = mutableListOf<String>()

    @After
    fun tearDown() {
        copied.forEach { (pkgName, previous) ->
            if (previous != null) {
                ExtensionsStorage.commit(storageDir, pkgName, previous)
            } else {
                ExtensionsStorage.delete(storageDir, pkgName)
            }
        }
        runBlocking {
            dataStore.edit { preferences ->
                preferences[TrustedExtensionsPreferences.TRUSTED_SIGNATURES] =
                    preferences[TrustedExtensionsPreferences.TRUSTED_SIGNATURES].orEmpty() - trusted.toSet()
            }
        }
    }

    @Test
    fun sampleOfTheCurrentLibLoadsAndAnswersThroughItsSuspendMethods(): Unit = runBlocking {
        val apk = File(context.cacheDir, "lib-sample.apk")
        testContext.assets.open("lib-sample.apk").use { input -> apk.outputStream().use { input.copyTo(it) } }
        val pkgInfo = ExtensionsLoader.getArchivePackageInfo(context, apk)!!
        // Signed with a debug key: trusted, as the user would
        trust(ExtensionsLoader.signatureHashes(pkgInfo).last())

        val extension = load(pkgInfo.packageName, apk)
        assertNull(extension.loadIssue)
        assertEquals(ExtensionsLoader.API_VERSION_MAX, extension.apiVersion)
        // An app accepting up to lib 1.6 refuses it
        assertEquals(
            ExtensionLoadIssue.LIB_TOO_NEW,
            ExtensionsLoader.libVersionIssue(LibVersion.fromVersionName(pkgInfo.versionName!!), max = LibVersion(1, 6)),
        )

        val source = extension.sources.single()
        assertTrue(source is ConfigurableAnimeHttpSource<*>)
        source as ConfigurableAnimeHttpSource<*>
        // Generated like tadami-extensions-source does, one of its golden vectors first
        assertEquals(6336613410238430609L, sourceId("tadamiorg/tadami-extensions-source", "com.sf.tadami.extension.en.example", "Example", 1))
        assertEquals(sourceId("tadamiorg/tadami", pkgInfo.packageName, "LibSample", 1), source.id)
        assertTrue(source.getPreferenceScreen().preferences.isNotEmpty())

        MockWebServer().use { server ->
            server.dispatcher = Site()
            server.start()
            val baseUrl = server.url("").toString().trimEnd('/')
            // Through its preferences, as the user would set them
            source.dataStore.edit { it[stringPreferencesKey("base_url")] = baseUrl }
            withTimeout(5_000) {
                while (source.baseUrl != baseUrl) kotlinx.coroutines.delay(50)
            }

            val latest = source.getLatestAnime(1)
            assertEquals(listOf("One", "Two"), latest.animes.map { it.title })
            assertEquals(listOf("Found q"), source.getSearchAnime(1, "q", source.getFilterList(), false).animes.map { it.title })

            val details = source.getAnimeDetails(latest.animes.first())
            assertEquals("The first anime", details.description)
            val episodes = source.getEpisodeList(details)
            assertEquals(listOf("/episode/1", "/episode/2"), episodes.map { it.url })
            val streams = source.getEpisodeSources(episodes.first())
            assertEquals(listOf("https://cdn.example/1.m3u8"), streams.map { it.url })

            try {
                source.getAnimeDetails(SAnime.create().apply { title = "Missing"; url = "/missing" })
                fail("No HttpException")
            } catch (e: HttpException) {
                assertEquals(404, e.code)
            }
        }
    }

    @Test
    fun publishedExtensionsLoadAndGiveTheirSettings(): Unit = runBlocking {
        val apks = try {
            publishedApks()
        } catch (e: Exception) {
            assumeTrue("The published extensions can't be downloaded: $e", false)
            return@runBlocking
        }
        assertTrue(apks.isNotEmpty())

        for ((name, file) in apks) {
            val pkgInfo = ExtensionsLoader.getArchivePackageInfo(context, file)!!
            val extension = load(pkgInfo.packageName, file)
            assertNull("$name: ${extension.loadIssue}", extension.loadIssue)
            assertTrue("$name has no source", extension.sources.isNotEmpty())
            extension.sources.forEach { source -> checkSettings(name, source) }
        }
    }

    /** What the screens read of a source, without calling its site */
    private fun checkSettings(name: String, source: Source) {
        try {
            source.name
            source.lang
            if (source is AnimeCatalogueSource) {
                source.getFilterList()
                source.supportSearch
                source.supportRecent
            }
            if (source is AnimeHttpSource) {
                source.baseUrl
                source.headers
                source.client
            }
            if (source is ConfigurableSource) {
                source.dataStore
                source.getPreferenceScreen()
            }
            if (source is ConfigurableParsedHttpAnimeSource<*>) {
                source.preferences
                source.preferencesState.value
            }
        } catch (e: Throwable) {
            throw AssertionError("$name: ${source.name} failed", e)
        }
    }

    /** Puts [apk] in the storage as the app does, then loads it */
    private fun load(pkgName: String, apk: File): Extension.Installed {
        if (pkgName !in copied) {
            // One the app already had is put back after the test
            copied[pkgName] = ExtensionsStorage.apkFile(storageDir, pkgName).takeIf { it.isFile }?.let { installed ->
                ExtensionsStorage.newTmpFile(storageDir, pkgName).also { installed.copyTo(it, overwrite = true) }
            }
        }
        val tmp = ExtensionsStorage.newTmpFile(storageDir, pkgName)
        apk.copyTo(tmp, overwrite = true)
        ExtensionsStorage.commit(storageDir, pkgName, tmp)

        val late = CompletableFuture<LoadResult>()
        // None of them conflicts: the source ID check is tested on its own
        val result = ExtensionsLoader.reloadExtension(context, pkgName, { _, _ -> emptyMap() }) { late.complete(it) }
        val loaded = (result as? LoadResult.Success)?.extension
            ?.takeIf { it.loadIssue == ExtensionLoadIssue.TIMED_OUT }
            ?.let { late.get(60, TimeUnit.SECONDS) }
            ?: result
        return (loaded as? LoadResult.Success)?.extension ?: throw AssertionError("$pkgName didn't load: $loaded")
    }

    private suspend fun trust(signatureHash: String) {
        trusted += signatureHash
        dataStore.edit { preferences ->
            preferences[TrustedExtensionsPreferences.TRUSTED_SIGNATURES] =
                preferences[TrustedExtensionsPreferences.TRUSTED_SIGNATURES].orEmpty() + signatureHash
        }
    }

    /** The APKs the index of the published repo lists, downloaded, by name */
    private fun publishedApks(): List<Pair<String, File>> {
        val client = OkHttpClient.Builder().callTimeout(2, TimeUnit.MINUTES).build()
        fun get(url: String): ByteArray = client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            check(response.isSuccessful) { "GET $url: HTTP ${response.code}" }
            response.body.bytes()
        }
        val index = Json.parseToJsonElement(get("$REPO/index.min.json").decodeToString()).jsonArray
        return index.map { entry ->
            val name = entry.jsonObject["apk"]!!.jsonPrimitive.content
            val file = File(context.cacheDir, "published/$name")
            if (!file.isFile) {
                file.parentFile!!.mkdirs()
                file.writeBytes(get("$REPO/apk/$name"))
            }
            name to file
        }
    }

    /** One "<title>|<url>" line per anime, one URL per line for the episodes and the streams */
    private class Site : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val body = when (request.url.encodedPath) {
                "/latest" -> "One|/anime/1\nTwo|/anime/2"
                "/search" -> "Found ${request.url.queryParameter("q")}|/anime/3"
                "/anime/1" -> "The first anime"
                "/anime/1/episodes" -> "/episode/1\n/episode/2"
                "/episode/1" -> "https://cdn.example/1.m3u8"
                else -> return MockResponse.Builder().code(404).build()
            }
            return MockResponse.Builder().body(body).build()
        }
    }

    companion object {
        private const val REPO = "https://raw.githubusercontent.com/tadamiorg/tadami-extensions/main"

        /** tadami-extensions-source's formula, see its source-ids.gradle */
        fun sourceId(repoId: String, pkgName: String, sourceKey: String, versionId: Int): Long {
            val md5 = MessageDigest.getInstance("MD5").digest("tadami-source/v1/$repoId/$pkgName/$sourceKey/$versionId".toByteArray())
            return ByteBuffer.wrap(md5, 0, 8).long and Long.MAX_VALUE
        }
    }
}
