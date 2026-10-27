package com.sf.tadami.ui.tabs.more.downloads

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.data.download.QueueOrder
import com.sf.tadami.data.download.TadamiDownloadManager
import com.sf.tadami.data.download.model.Download
import com.sf.tadami.preferences.download.DownloadPreferences
import com.sf.tadami.utils.getPreferencesGroupAsFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data class DownloadQueueUiState(
    val queue: List<Download> = emptyList(),
    val hasQueued: Boolean = false,
    val hasErrors: Boolean = false,
    /** Running or waiting for its network: not paused */
    val isScheduled: Boolean = false,
    val isRunning: Boolean = false,
    val onlyOverWifi: Boolean = true,
)

class DownloadQueueViewModel : ViewModel() {
    private val downloadManager: TadamiDownloadManager = Injekt.get()
    private val dataStore: DataStore<Preferences> = Injekt.get()

    val uiState: StateFlow<DownloadQueueUiState> = combine(
        downloadManager.queueState,
        // The statuses change without the queue
        downloadManager.statusFlow().map { }.onStart { emit(Unit) },
        downloadManager.isRunning,
        downloadManager.isScheduledFlow(),
        dataStore.getPreferencesGroupAsFlow(DownloadPreferences).map { it.onlyOverWifi },
    ) { queue, _, isRunning, isScheduled, onlyOverWifi ->
        DownloadQueueUiState(
            queue = queue,
            hasQueued = queue.any { it.status == Download.State.QUEUED || it.status == Download.State.DOWNLOADING },
            hasErrors = queue.any { it.status == Download.State.ERROR },
            isScheduled = isScheduled,
            isRunning = isRunning,
            onlyOverWifi = onlyOverWifi,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DownloadQueueUiState(
            queue = downloadManager.queueState.value,
            isScheduled = downloadManager.isRunning.value,
            isRunning = downloadManager.isRunning.value,
        ),
    )

    fun pause() = downloadManager.pauseDownloads()

    fun resume() = downloadManager.startDownloads()

    fun cancelAll() = downloadManager.clearQueue()

    fun clearErrors() {
        downloadManager.cancelQueuedDownloads(downloadManager.queueState.value.filter { it.status == Download.State.ERROR })
    }

    fun cancel(download: Download) = downloadManager.cancelQueuedDownloads(listOf(download))

    fun retry(download: Download) = downloadManager.retryDownloads(listOf(download))

    fun moveToTop(download: Download) = downloadManager.moveToTop(download)

    fun moveToBottom(download: Download) {
        downloadManager.reorderQueue(QueueOrder.toBottom(downloadManager.queueState.value, listOf(download)))
    }
}
