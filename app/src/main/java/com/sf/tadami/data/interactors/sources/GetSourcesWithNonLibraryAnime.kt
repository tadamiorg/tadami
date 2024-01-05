package com.sf.tadami.data.interactors.sources

import com.sf.tadami.data.sources.SourceRepository
import com.sf.tadami.data.sources.SourceWithCount
import kotlinx.coroutines.flow.Flow

class GetSourcesWithNonLibraryAnime(
    private val repository: SourceRepository,
) {

    fun subscribe(): Flow<List<SourceWithCount>> {
        return repository.getSourcesWithNonLibraryAnime()
    }

    suspend fun delete(sourcesIds : List<String>){
        repository.deleteAnimesNotInLibraryBySourceIds(sourcesIds)
    }
}