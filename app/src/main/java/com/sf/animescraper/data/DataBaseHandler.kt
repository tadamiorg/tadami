package com.sf.animescraper.data

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.Query
import com.sf.animescraper.Database
import kotlinx.coroutines.flow.Flow

interface DataBaseHandler {
    suspend fun <T> await(block: Database.() -> T) : T

    suspend fun <T : Any> awaitList(
        block: Database.() -> Query<T>,
    ): List<T>

    suspend fun <T : Any> awaitOne(
        inTransaction: Boolean = false,
        block: Database.() -> Query<T>,
    ): T

    suspend fun <T : Any> awaitOneOrNull(
        block: Database.() -> ExecutableQuery<T>,
    ): T?

    fun <T : Any> subscribeToList(block: Database.() -> Query<T>): Flow<List<T>>

    fun <T : Any> subscribeToOne(block: Database.() -> Query<T>): Flow<T>

    fun <T : Any> subscribeToOneOrNull(block: Database.() -> Query<T>): Flow<T?>
}