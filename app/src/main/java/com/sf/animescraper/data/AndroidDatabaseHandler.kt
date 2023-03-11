package com.sf.animescraper.data

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.sf.animescraper.Database
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AndroidDatabaseHandler(
    private val db: Database,
    private val queryDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DataBaseHandler {
    override suspend fun <T> await(block: Database.() -> T) : T {
        return withContext(queryDispatcher){
            db.transactionWithResult {
                block(db)
            }
        }
    }

    override suspend fun <T : Any> awaitList(block: Database.() -> Query<T>): List<T> {
        return withContext(queryDispatcher){
            db.transactionWithResult {
                block(db).executeAsList()
            }
        }
    }

    override suspend fun <T : Any> awaitOne(
        inTransaction: Boolean,
        block: Database.() -> Query<T>
    ): T {
        return withContext(queryDispatcher){
            db.transactionWithResult {
                block(db).executeAsOne()
            }
        }
    }

    override suspend fun <T : Any> awaitOneOrNull(block: Database.() -> ExecutableQuery<T>): T? {
        return withContext(queryDispatcher){
            db.transactionWithResult {
                block(db).executeAsOneOrNull()
            }
        }
    }

    override fun <T : Any> subscribeToList(block: Database.() -> Query<T>): Flow<List<T>> {
        return block(db).asFlow().mapToList(queryDispatcher)
    }

    override fun <T : Any> subscribeToOne(block: Database.() -> Query<T>): Flow<T> {
        return block(db).asFlow().mapToOne(queryDispatcher)
    }

    override fun <T : Any> subscribeToOneOrNull(block: Database.() -> Query<T>): Flow<T?> {
        return block(db).asFlow().mapToOneOrNull(queryDispatcher)
    }

}