package com.sf.tadami.preferences.model

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.preferences.library.LibrarySort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Test

class DataStoreStateTest {

    // The file backed DataStore can't replace its file on Windows, an in memory one is enough here
    private class InMemoryDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        private val mutex = Mutex()
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            mutex.withLock { transform(state.value).also { state.value = it } }
    }

    @Test
    fun settingAValueKeepsAKeyWrittenElsewhereMeanwhile() = runBlocking {
        val dataStore = InMemoryDataStore()
        dataStore.edit { it[LibraryPreferences.NEW_UPDATES_COUNT] = 1 }
        val state = DataStoreState(dataStore, LibraryPreferences)
        val staleCopy = state.value.value

        // The library update worker counts new episodes after the copy was read
        dataStore.edit { it[LibraryPreferences.NEW_UPDATES_COUNT] = 5 }
        state.setValue(staleCopy.copy(sortFlags = LibrarySort(LibrarySort.UNSEEN_COUNT)))

        val stored = LibraryPreferences.transform(dataStore.data.first())
        assertEquals(5, stored.newUpdatesCount)
        assertEquals(LibrarySort.UNSEEN_COUNT, stored.sortFlags.flags)
    }

    @Test
    fun settingAValueStillStoresTheKeysNotStoredYet() = runBlocking {
        val dataStore = InMemoryDataStore()
        val state = DataStoreState(dataStore, LibraryPreferences)

        state.setValue(state.value.value)

        // Saving the group still stores its defaults, like writing the whole group did
        assertEquals(0, dataStore.data.first()[LibraryPreferences.NEW_UPDATES_COUNT])
    }
}
