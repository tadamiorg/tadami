package com.sf.tadami.data.sources

import com.sf.tadami.data.DataBaseHandler
import com.sf.tadami.source.online.ConfigurableParsedHttpAnimeSource
import com.sf.tadami.source.Source
import com.sf.tadami.source.StubSource
import com.sf.tadami.ui.tabs.browse.SourceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.sf.tadami.domain.source.Source as DomainSource

interface SourceRepository {
    fun getSources(): Flow<List<DomainSource>>

    fun getSourcesWithNonLibraryAnime(): Flow<List<SourceWithCount>>

    suspend fun deleteAnimesNotInLibraryBySourceIds(sourceIds : List<Long>)
}

class SourceRepositoryImpl(
    private val sourceManager: SourceManager,
    private val handler: DataBaseHandler
) : SourceRepository
{

    override fun getSources(): Flow<List<DomainSource>> {
        return sourceManager.catalogueSources.map { sources ->
            sources.map {
                mapSourceToDomainSource(it).copy(
                    supportsLatest = it.supportRecent,
                    isConfigurable = it is ConfigurableParsedHttpAnimeSource<*>
                )
            }
        }
    }
    override fun getSourcesWithNonLibraryAnime(): Flow<List<SourceWithCount>> {
        val sourceIdWithNonLibraryAnime = handler.subscribeToList { animeQueries.getSourceIdsWithNonLibraryAnime() }
        return sourceIdWithNonLibraryAnime.map { sourceId ->
            sourceId.map { (sourceId, count) ->
                val source = sourceManager.getOrStub(sourceId)
                val domainSource = mapSourceToDomainSource(source).copy(
                    isStub = source is StubSource,
                )
                SourceWithCount(domainSource, count)
            }
        }
    }

    override suspend fun deleteAnimesNotInLibraryBySourceIds(sourceIds: List<Long>) {
        handler.await {
            animeQueries.deleteAnimesNotInLibraryBySourceIds(sourceIds)
        }
    }

    private fun mapSourceToDomainSource(source: Source): DomainSource = DomainSource(
        id = source.id,
        lang = source.lang,
        name = source.name,
        supportsLatest = false,
        isStub = false,
        isConfigurable = false
    )
}



data class SourceWithCount(
    val source: DomainSource,
    val count: Long,
) {

    val id: Long
        get() = source.id

    val name: String
        get() = source.name
}
