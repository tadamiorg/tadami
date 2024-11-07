package com.sf.tadami.domain.storage

import android.content.Context
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.hippo.unifile.UniFile
import com.sf.tadami.preferences.storage.StoragePreferences
import com.sf.tadami.utils.DiskUtil
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.getPreferencesGroupAsFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.runBlocking

class StorageManager(
    private val context: Context,
    private val dataStore: DataStore<Preferences>
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private var baseDir: UniFile? = getBaseDir(runBlocking {
        dataStore.getPreferencesGroup(StoragePreferences).storageDir
    })

    private val _changes: Channel<Unit> = Channel(Channel.UNLIMITED)
    val changes = _changes.receiveAsFlow()
        .shareIn(scope, SharingStarted.Lazily, 1)

    init {
        dataStore.getPreferencesGroupAsFlow(StoragePreferences)
            .drop(1)
            .distinctUntilChanged()
            .onEach { prefs ->
                baseDir = getBaseDir(prefs.storageDir)
                baseDir?.let { parent ->
                    parent.createDirectory(AUTOMATIC_BACKUPS_PATH)
                    parent.createDirectory(LOCAL_SOURCE_PATH)
                    parent.createDirectory(DOWNLOADS_PATH).also {
                        DiskUtil.createNoMediaFile(it, context)
                    }
                }
                _changes.send(Unit)
            }
            .launchIn(scope)
    }

    private fun getBaseDir(uri: String): UniFile? {
        return UniFile.fromUri(context, uri.toUri())
            .takeIf { it?.exists() == true }
    }

    fun getAutomaticBackupsDirectory(): UniFile? {
        return baseDir?.createDirectory(AUTOMATIC_BACKUPS_PATH)
    }

    fun getDownloadsDirectory(): UniFile? {
        return baseDir?.createDirectory(DOWNLOADS_PATH)
    }

    fun getLocalSourceDirectory(): UniFile? {
        return baseDir?.createDirectory(LOCAL_SOURCE_PATH)
    }
}

private const val AUTOMATIC_BACKUPS_PATH = "autobackup"
private const val DOWNLOADS_PATH = "downloads"
private const val LOCAL_SOURCE_PATH = "local"