package com.sf.tadami.ui.tabs.browse

import android.content.Context
import com.sf.tadami.data.download.TadamiDownloadManager
import com.sf.tadami.data.sources.StubSourceRepository
import com.sf.tadami.extension.ExtensionManager
import com.sf.tadami.source.AnimeCatalogueSource
import com.sf.tadami.source.Source
import com.sf.tadami.source.StubSource
import com.sf.tadami.source.online.AnimeHttpSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set


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

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private val sourcesMapFlow = MutableStateFlow(ConcurrentHashMap<Long, Source>())

    private val stubSourcesMap = ConcurrentHashMap<Long, StubSource>()

    override val catalogueSources: Flow<List<AnimeCatalogueSource>> = sourcesMapFlow.map {
        it.values.filterIsInstance<AnimeCatalogueSource>()
    }

    init {
        scope.launch {
            extensionManager.installedExtensionsFlow
                .collectLatest { extensions ->
                    val mutableMap = ConcurrentHashMap<Long, Source>(
                        mapOf(),
                    )
                    extensions.forEach { extension ->
                        extension.sources.forEach {
                            mutableMap[it.id] = it
                            registerStubSource(StubSource.from(it))
                        }
                    }
                    sourcesMapFlow.value = mutableMap
                }
        }

        scope.launch {
            sourceRepository.subscribeAll()
                .collectLatest { sources ->
                    val mutableMap = stubSourcesMap.toMutableMap()
                    sources.forEach {
                        mutableMap[it.id] = it
                    }
                }
        }
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

    private fun registerStubSource(source: StubSource) {
        scope.launch {
            val dbSource = sourceRepository.getStubSource(source.id)
            if (dbSource == source) return@launch
            sourceRepository.upsertStubSource(source.id, source.lang.name, source.name)
            if (dbSource != null) {
                tadamiDownloadManager.renameSource(dbSource, source)
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