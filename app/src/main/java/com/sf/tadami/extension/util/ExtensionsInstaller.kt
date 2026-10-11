package com.sf.tadami.extension.util

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.installer.Installer
import com.sf.tadami.extension.model.InstallStep
import com.sf.tadami.notifications.extensionsinstaller.ExtensionInstallService
import com.sf.tadami.preferences.extensions.ExtensionInstallerEnum
import com.sf.tadami.preferences.extensions.ExtensionsPreferences
import com.sf.tadami.ui.utils.getUriCompat
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.isPackageInstalled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

/**
 * The installer which installs, updates and uninstalls the extensions.
 *
 * @param context The application context.
 */
internal class ExtensionsInstaller(private val context: Context) {

    private val dataStore : DataStore<Preferences> = Injekt.get()
    /**
     * The system's download manager
     */
    private val downloadManager = context.getSystemService<DownloadManager>()!!

    /**
     * The broadcast receiver which listens to download completion events.
     */
    private val downloadReceiver = DownloadCompletionReceiver()

    /**
     * The currently requested downloads, with the package name (unique id) as key, and the id
     * returned by the download manager.
     */
    private val activeDownloads = ConcurrentHashMap<String, Long>()

    private val downloadsStateFlows = ConcurrentHashMap<Long, MutableStateFlow<InstallStep>>()

    /**
     * Guards the changes of [activeDownloads] made together with the receiver registration, as
     * downloads are started from several IO threads while completions are handled on main.
     */
    private val lock = Any()

    private val extensionInstaller = runBlocking {
        dataStore.getPreferencesGroup(ExtensionsPreferences).extensionInstallerEnum
    }

    /**
     * Adds the given extension to the downloads queue and returns an observable containing its
     * step in the installation process.
     *
     * @param url The url of the apk.
     * @param extension The extension to install.
     */
    fun downloadAndInstall(url: String, extension: Extension): Flow<InstallStep> {
        val pkgName = extension.pkgName

        val downloadUri = url.toUri()
        val request = DownloadManager.Request(downloadUri)
            .setTitle(extension.name)
            .setMimeType(APK_MIME)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, downloadUri.lastPathSegment)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val downloadStateFlow = MutableStateFlow(InstallStep.Pending)

        val id = synchronized(lock) {
            val oldDownload = activeDownloads[pkgName]
            if (oldDownload != null) {
                deleteDownload(pkgName)
            }

            // Register the receiver after removing (and unregistering) the previous download
            downloadReceiver.register()

            downloadManager.enqueue(request).also {
                activeDownloads[pkgName] = it
                downloadsStateFlows[it] = downloadStateFlow
            }
        }

        // Poll download status
        val pollStatusFlow = downloadStatusFlow(id).mapNotNull { downloadStatus ->
            // Map to our model
            when (downloadStatus) {
                DownloadManager.STATUS_PENDING -> InstallStep.Pending
                DownloadManager.STATUS_RUNNING -> InstallStep.Downloading
                else -> null
            }
        }

        return merge(downloadStateFlow, pollStatusFlow).transformWhile {
            emit(it)
            // Stop when the application is installed or errors
            !it.isCompleted()
        }.onCompletion {
            // Always notify on main thread
            withContext(Dispatchers.Main) {
                // Always remove the download when unsubscribed
                deleteDownload(pkgName, id)
            }
        }
    }

    /**
     * Returns a flow that polls the given download id for its status every second, as the
     * manager doesn't have any notification system. It'll stop once the download finishes.
     *
     * @param id The id of the download to poll.
     */
    private fun downloadStatusFlow(id: Long): Flow<Int> = flow {
        val query = DownloadManager.Query().setFilterById(id)
        while (true) {
            // Get the current download status
            val downloadStatus = downloadManager.query(query).use { cursor ->
                if (!cursor.moveToFirst()) return@flow
                cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            }

            emit(downloadStatus)

            // Stop polling when the download fails or finishes
            if (
                downloadStatus == DownloadManager.STATUS_SUCCESSFUL ||
                downloadStatus == DownloadManager.STATUS_FAILED
            ) {
                return@flow
            }

            delay(1.seconds)
        }
    }
        // Ignore duplicate results
        .distinctUntilChanged()

    /**
     * Starts an intent to install the extension at the given uri.
     *
     * @param uri The uri of the extension to install.
     */
    fun installApk(downloadId: Long, uri: Uri) {
        when (val installer = extensionInstaller) {
            ExtensionInstallerEnum.LEGACY -> {
                val intent = Intent(context, ExtensionInstallActivity::class.java)
                    .setDataAndType(uri, APK_MIME)
                    .putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)

                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Log.d("ExtensionsInstaller - installApk","Couldn't start the install activity")
                    updateInstallStep(downloadId, InstallStep.Error)
                }
            }
            else -> {
                val intent = ExtensionInstallService.getIntent(context, downloadId, uri, installer)
                try {
                    ContextCompat.startForegroundService(context, intent)
                } catch (e: IllegalStateException) {
                    // ForegroundServiceStartNotAllowedException when the app is in the background
                    Log.d("ExtensionsInstaller - installApk","Couldn't start the install service")
                    updateInstallStep(downloadId, InstallStep.Error)
                }
            }
        }
    }

    /**
     * Cancels extension install and remove from download manager and installer.
     */
    fun cancelInstall(pkgName: String) {
        val downloadId = synchronized(lock) {
            val id = activeDownloads.remove(pkgName) ?: return
            downloadManager.remove(id)
            if (activeDownloads.isEmpty()) {
                downloadReceiver.unregister()
            }
            id
        }
        // Emit a terminal step so the install flow completes and cleans up
        downloadsStateFlows.remove(downloadId)?.value = InstallStep.Idle
        Installer.cancelInstallQueue(downloadId)
    }

    /**
     * Starts an intent to uninstall the extension by the given package name.
     *
     * @param pkgName The package name of the extension to uninstall
     */
    fun uninstallApk(pkgName: String) {
        if (context.isPackageInstalled(pkgName)) {
            @Suppress("DEPRECATION")
            val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, "package:$pkgName".toUri())
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    /**
     * Sets the step of the installation of an extension.
     *
     * @param downloadId The id of the download.
     * @param step New install step.
     */
    fun updateInstallStep(downloadId: Long, step: InstallStep) {
        downloadsStateFlows[downloadId]?.let { it.value = step }
    }

    /**
     * Deletes the download for the given package name.
     *
     * @param pkgName The package name of the download to delete.
     * @param downloadId If set, only deletes the download if it is still the active one for this
     * package (it may have been cancelled and requested again since).
     */
    private fun deleteDownload(pkgName: String, downloadId: Long? = null) {
        synchronized(lock) {
            val removedId = if (downloadId == null) {
                activeDownloads.remove(pkgName)
            } else {
                downloadId.takeIf { activeDownloads.remove(pkgName, it) }
            }
            if (removedId != null) {
                downloadManager.remove(removedId)
                downloadsStateFlows.remove(removedId)
            }
            if (activeDownloads.isEmpty()) {
                downloadReceiver.unregister()
            }
        }
    }

    /**
     * Receiver that listens to download status events.
     */
    private inner class DownloadCompletionReceiver : BroadcastReceiver() {

        /**
         * Whether this receiver is currently registered. Only changed while holding [lock].
         */
        private var isRegistered = false

        /**
         * Registers this receiver if it's not already.
         */
        fun register() {
            if (isRegistered) return
            isRegistered = true

            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            ContextCompat.registerReceiver(context, this, filter, ContextCompat.RECEIVER_EXPORTED)
        }

        /**
         * Unregisters this receiver if it's not already.
         */
        fun unregister() {
            if (!isRegistered) return
            isRegistered = false

            context.unregisterReceiver(this)
        }

        /**
         * Called when a download event is received. It looks for the download in the current active
         * downloads and notifies its installation step.
         */
        override fun onReceive(context: Context, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0) ?: return
            // Avoid events for downloads we didn't request
            if (synchronized(lock) { id !in activeDownloads.values }) return

            val uri = downloadManager.getUriForDownloadedFile(id)

            // Set next installation step
            if (uri == null) {
                Log.d("DownloadCompletionReceiver - onReceive","Couldn't locate downloaded APK")
                updateInstallStep(id, InstallStep.Error)
                return
            }

            val query = DownloadManager.Query().setFilterById(id)
            downloadManager.query(query).use { cursor ->
                if (cursor.moveToFirst()) {
                    val localUri = cursor.getString(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI),
                    ).removePrefix(FILE_SCHEME)
                    installApk(id, File(localUri).getUriCompat(context))
                }
            }
        }
    }

    companion object {
        const val APK_MIME = "application/vnd.android.package-archive"
        const val EXTRA_DOWNLOAD_ID = "ExtensionInstaller.extra.DOWNLOAD_ID"
        const val FILE_SCHEME = "file://"
    }
}
