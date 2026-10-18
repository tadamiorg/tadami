package com.sf.tadami.ui.tabs.browse

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.sf.tadami.data.download.TadamiDownloadManager
import com.sf.tadami.data.sources.StubSourceRepository
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.ExtensionManager
import com.sf.tadami.preferences.extensions.SourceIdOwnersPreferences
import com.sf.tadami.source.AnimeCatalogueSource
import com.sf.tadami.source.Source
import com.sf.tadami.source.StubSource
import com.sf.tadami.source.online.AnimeHttpSource
import com.sf.tadami.source.online.ConfigurableAnimeHttpSource
import com.sf.tadami.source.online.ConfigurableParsedHttpAnimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.ConcurrentHashMap


interface SourceManager {
    val catalogueSources: Flow<List<AnimeCatalogueSource>>

    fun get(sourceKey: Long): Source?

    fun getOrStub(sourceKey: Long, name : String? = null): Source

    fun getOnlineSources(): List<AnimeHttpSource>

    fun getCatalogueSources(): List<AnimeCatalogueSource>

    fun getStubSources(): List<StubSource>
}
class SourceManagerImplementation(
    private val context: Context,
    private val extensionManager: ExtensionManager,
    private val sourceRepository: StubSourceRepository,
) : SourceManager {

    private val tadamiDownloadManager: TadamiDownloadManager by injectLazy()

    private val dataStore: DataStore<Preferences> by injectLazy()

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private val sourcesMapFlow = MutableStateFlow(ConcurrentHashMap<Long, Source>())

    private val stubSourcesMap = ConcurrentHashMap<Long, StubSource>()

    override val catalogueSources: Flow<List<AnimeCatalogueSource>> = sourcesMapFlow.map {
        it.values.filterIsInstance<AnimeCatalogueSource>()
    }

    init {
        // The extensions are already loaded, fill the map right away so the sources are available
        // as soon as the manager is built
        sourcesMapFlow.value = buildSourcesMap(extensionManager.installedExtensionsFlow.value)

        scope.launch {
            extensionManager.installedExtensionsFlow
                .collectLatest { extensions ->
                    val mutableMap = buildSourcesMap(extensions)
                    extensions.forEach { extension ->
                        extension.sources
                            .filter { mutableMap[it.id] === it }
                            .forEach { registerStubSource(StubSource.from(it), extension.pkgName) }
                    }
                    sourcesMapFlow.value.forEach { (id, source) ->
                        // Stop the preferences updates of replaced or removed instances. Their
                        // preferences are kept on disk so they survive a reinstall.
                        if (mutableMap[id] !== source) {
                            when (source) {
                                is ConfigurableParsedHttpAnimeSource<*> -> source.cancelPreferencesUpdates()
                                is ConfigurableAnimeHttpSource<*> -> source.cancelPreferencesUpdates()
                            }
                        }
                    }
                    sourcesMapFlow.value = mutableMap
                }
        }

        scope.launch {
            sourceRepository.subscribeAll()
                .collectLatest { sources ->
                    stubSourcesMap.putAll(sources.associateBy { it.id })
                }
        }
    }

    /**
     * The sources of [extensions] by ID. ExtensionManager refuses the extensions whose source IDs
     * another one owns: should two sources still have the same ID, the first one stays, never replaced.
     */
    private fun buildSourcesMap(extensions: List<Extension.Installed>): ConcurrentHashMap<Long, Source> {
        val mutableMap = ConcurrentHashMap<Long, Source>()
        extensions.forEach { extension ->
            extension.sources.forEach {
                mutableMap.putIfAbsent(it.id, it)
            }
        }
        return mutableMap
    }

    override fun get(sourceKey: Long): Source? {
        return sourcesMapFlow.value[sourceKey]
    }

    override fun getOrStub(sourceKey: Long, name : String?): Source {
        return sourcesMapFlow.value[sourceKey] ?: stubSourcesMap.getOrPut(sourceKey) {
            runBlocking { createStubSource(sourceKey,name) }
        }
    }

    override fun getOnlineSources() = sourcesMapFlow.value.values.filterIsInstance<AnimeHttpSource>()

    override fun getCatalogueSources() = sourcesMapFlow.value.values.filterIsInstance<AnimeCatalogueSource>()

    override fun getStubSources(): List<StubSource> {
        val onlineSourceIds = getOnlineSources().map { it.id }
        return stubSourcesMap.values.filterNot { it.id in onlineSourceIds }
    }

    /**
     * Stores [source], the name and language shown once its extension is uninstalled. When they
     * changed, its downloads, stored under them, are renamed, if the stored one was a source of the
     * same extension [ownerPkgName]: another extension now owning its ID, after an uninstall, has its
     * own downloads. Null when unknown, from a repo for instance.
     */
    private fun registerStubSource(source: StubSource, ownerPkgName: String? = null) {
        scope.launch {
            val dbSource = sourceRepository.getStubSource(source.id)
            val stubOwner = dataStore.data.first()[SourceIdOwnersPreferences.STUB_OWNERS]
                .let(SourceIdOwnersPreferences::decode)[source.id]
            // Unknown, stored by an older version: of the same extension, as it then was
            val sameOwner = stubOwner == null || ownerPkgName == null || stubOwner == ownerPkgName
            // StubSource has no equals, compare its fields
            if (dbSource == null || dbSource.name != source.name || dbSource.lang != source.lang) {
                sourceRepository.upsertStubSource(source.id, source.lang.name, source.name)
                if (dbSource != null && sameOwner) {
                    tadamiDownloadManager.renameSource(dbSource, source)
                }
            }
            if (ownerPkgName != null && stubOwner != ownerPkgName) {
                dataStore.edit { preferences ->
                    val owners = SourceIdOwnersPreferences.decode(preferences[SourceIdOwnersPreferences.STUB_OWNERS])
                    preferences[SourceIdOwnersPreferences.STUB_OWNERS] =
                        SourceIdOwnersPreferences.encode(owners + (source.id to ownerPkgName))
                }
            }
        }
    }

    private suspend fun createStubSource(id: Long, name: String? = null): StubSource {
        sourceRepository.getStubSource(id)?.let {
            return it
        }
        extensionManager.getSourceData(id)?.let {
            registerStubSource(it)
            return it
        }
        return StubSource(id = id, name = name ?: "")
    }

}