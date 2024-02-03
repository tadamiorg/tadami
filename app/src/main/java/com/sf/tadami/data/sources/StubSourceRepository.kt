package com.sf.tadami.data.sources

import com.sf.tadami.data.DataBaseHandler
import com.sf.tadami.source.online.StubSource
import com.sf.tadami.utils.Lang
import kotlinx.coroutines.flow.Flow


interface StubSourceRepository {
    fun subscribeAll(): Flow<List<StubSource>>

    suspend fun getStubSource(id: Long): StubSource?

    suspend fun upsertStubSource(id: Long, lang: String, name: String)
}

class StubSourceRepositoryImpl(
    private val handler: DataBaseHandler,
) : StubSourceRepository {

    override fun subscribeAll(): Flow<List<StubSource>> {
        return handler.subscribeToList { sourceQueries.findAll(::mapStubSource) }
    }

    override suspend fun getStubSource(id: Long): StubSource? {
        return handler.awaitOneOrNull { sourceQueries.findOne(id, ::mapStubSource) }
    }

    override suspend fun upsertStubSource(id: Long, lang: String, name: String) {
        handler.await { sourceQueries.upsert(id, lang, name) }
    }

    private fun mapStubSource(
        id: Long,
        lang: String,
        name: String,
    ): StubSource = StubSource(id = id, lang = Lang.valueOfOrDefault(lang), name = name)
}