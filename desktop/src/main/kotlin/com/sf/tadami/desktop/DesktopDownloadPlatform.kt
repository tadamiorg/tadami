package com.sf.tadami.desktop

import com.sf.tadami.data.download.DownloadPlatform
import com.sf.tadami.data.download.TadamiDownloadManager
import com.sf.tadami.data.download.model.Download
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

/** The download queue runs in a coroutine of the app while it's open, as the Android worker would. */
class DesktopDownloadPlatform : DownloadPlatform {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private val scheduled = MutableStateFlow(false)

    override val filesDir: File get() = AppDirs.data

    @Synchronized
    override fun startDownloads(onlyOverWifi: Boolean) {
        job?.cancel()
        val downloadManager = Injekt.get<TadamiDownloadManager>()
        job = scope.launch {
            scheduled.value = true
            try {
                val errors = launch {
                    downloadManager.statusFlow()
                        .filter { it.status == Download.State.ERROR }
                        .collect { downloadManager.showError(it) }
                }
                downloadManager.runQueue()
                errors.cancel()
            } finally {
                scheduled.value = false
            }
        }
    }

    @Synchronized
    override fun stopDownloads() {
        job?.cancel()
        job = null
        scheduled.value = false
    }

    override fun isDownloadScheduled(): Boolean = scheduled.value

    override fun downloadScheduledFlow(): Flow<Boolean> = scheduled
}
