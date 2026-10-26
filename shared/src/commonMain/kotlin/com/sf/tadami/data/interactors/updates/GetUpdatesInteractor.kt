package com.sf.tadami.data.interactors.updates

import com.sf.tadami.data.updates.UpdatesRepository
import com.sf.tadami.domain.updates.UpdatesWithRelations
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class GetUpdatesInteractor(
    private val repository: UpdatesRepository,
) {

    suspend fun await(seen: Boolean, after: Long): List<UpdatesWithRelations> {
        return repository.awaitWithSeen(seen, after, limit = 500)
    }

    fun subscribe(instant: Instant): Flow<List<UpdatesWithRelations>> {
        return repository.subscribeAll(instant.toEpochMilli(), limit = 500)
    }

    fun subscribe(seen: Boolean, after: Long): Flow<List<UpdatesWithRelations>> {
        return repository.subscribeWithSeen(seen, after, limit = 500)
    }
}