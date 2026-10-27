package com.sf.tadami.data.download

import android.app.Application
import android.media.MediaMetadataRetriever
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.data.download.model.Download
import com.sf.tadami.notifications.download.DownloadNotifier
import com.sf.tadami.notifications.download.DownloadWorker
import com.sf.tadami.storage.StorageFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * The downloads on Android: the queue runs in a WorkManager worker, with its notifications; the
 * preview frames are made by Media3 decoders.
 */
class AndroidDownloadPlatform(private val app: Application) : DownloadPlatform {

    private val notifier by lazy { DownloadNotifier(app) }

    override val filesDir: File get() = app.filesDir

    override fun startDownloads(onlyOverWifi: Boolean) = DownloadWorker.start(app, onlyOverWifi)

    override fun stopDownloads() = DownloadWorker.stop(app)

    override fun isDownloadScheduled(): Boolean = DownloadWorker.isScheduled(app)

    override fun downloadScheduledFlow(): Flow<Boolean> = DownloadWorker.isScheduledFlow(app)

    override fun showPaused(queued: Int) = notifier.showPaused(queued)

    override fun dismissPaused() = notifier.dismissPaused()

    override fun dismissProgress() = notifier.dismissProgress()

    override fun showError(download: Download) = notifier.showError(download)

    override fun dismissError() = notifier.dismissError()

    override fun videoDurationMs(file: StorageFile): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(app, file.uri.toUri())
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } finally {
            retriever.release()
        }
    }

    override fun previewMaker(
        provider: DownloadProvider,
        store: File,
        dataStore: DataStore<Preferences>,
        scope: CoroutineScope,
    ): DownloadPreviewMaker = PreviewGenerator.create(app, provider, store, dataStore, scope)
}
