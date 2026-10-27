package com.sf.tadami.data.download

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.data.download.model.Download
import com.sf.tadami.storage.StorageFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * What the downloads need from the platform the app runs on, registered in Injekt: how the queue runs
 * in the background (a WorkManager worker on Android), its notifications, and the media tools.
 */
interface DownloadPlatform {

    /** The app's private files directory: the queue and the resume points live under it. */
    val filesDir: File

    /** Runs the queue in the background, once a network is there ([onlyOverWifi]: an unmetered one). */
    fun startDownloads(onlyOverWifi: Boolean)

    /** Stops running the queue. */
    fun stopDownloads()

    /** Whether the queue runs or waits for its network: false once paused. */
    fun isDownloadScheduled(): Boolean

    /** See [isDownloadScheduled]. */
    fun downloadScheduledFlow(): Flow<Boolean>

    /** The queue is paused with [queued] downloads in it. */
    fun showPaused(queued: Int) = Unit

    fun dismissPaused() = Unit

    fun dismissProgress() = Unit

    /** [download] failed. */
    fun showError(download: Download) = Unit

    fun dismissError() = Unit

    /** The duration of the video [file], in milliseconds, null when it can't be read. */
    fun videoDurationMs(file: StorageFile): Long? = null

    /**
     * Makes the seekbar preview frames of the downloaded episodes, the queue of those waiting kept in
     * [store]: null when the platform makes none.
     */
    fun previewMaker(provider: DownloadProvider, store: File, dataStore: DataStore<Preferences>, scope: CoroutineScope): DownloadPreviewMaker? = null
}

/** Makes the seekbar preview frames of the downloaded episodes. */
interface DownloadPreviewMaker {

    /** The episode in `<sourceDir>/<animeDir>/<episodeDir>` was just downloaded. */
    fun onDownloaded(sourceDir: String, animeDir: String, episodeDir: String)

    /** Makes the frames of every episode waiting for them. */
    suspend fun makeAll()
}
