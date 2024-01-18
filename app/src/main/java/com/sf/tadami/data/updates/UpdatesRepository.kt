package com.sf.tadami.data.updates

import com.sf.tadami.data.DataBaseHandler
import com.sf.tadami.domain.updates.UpdatesWithRelations
import kotlinx.coroutines.flow.Flow

interface UpdatesRepository {

    suspend fun awaitWithSeen(seen: Boolean, after: Long, limit: Long): List<UpdatesWithRelations>

    fun subscribeAll(after: Long, limit: Long): Flow<List<UpdatesWithRelations>>

    fun subscribeWithSeen(seen: Boolean, after: Long, limit: Long): Flow<List<UpdatesWithRelations>>
}

class UpdatesRepositoryImpl(
    private val databaseHandler: DataBaseHandler,
) : UpdatesRepository {

    override suspend fun awaitWithSeen(
        seen: Boolean,
        after: Long,
        limit: Long,
    ): List<UpdatesWithRelations> {
        return databaseHandler.awaitList {
            updatesViewQueries.getUpdatesBySeenStatus(
                seen = seen,
                after = after,
                limit = limit,
                mapper = UpdatesMapper::mapUpdatesWithRelations
            )
        }
    }

    override fun subscribeAll(after: Long, limit: Long): Flow<List<UpdatesWithRelations>> {
        return databaseHandler.subscribeToList {
            updatesViewQueries.getRecentUpdates(after, limit, UpdatesMapper::mapUpdatesWithRelations)
        }
    }

    override fun subscribeWithSeen(
        seen: Boolean,
        after: Long,
        limit: Long,
    ): Flow<List<UpdatesWithRelations>> {
        return databaseHandler.subscribeToList {
            updatesViewQueries.getUpdatesBySeenStatus(
                seen = seen,
                after = after,
                limit = limit,
                mapper = UpdatesMapper::mapUpdatesWithRelations,
            )
        }
    }
}