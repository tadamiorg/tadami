package com.sf.tadami.notifications.extensionsinstaller

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.Log
import com.sf.tadami.extension.installer.Installer
import com.sf.tadami.extension.installer.TadamiPackageInstaller
import com.sf.tadami.extension.util.ExtensionsInstaller
import com.sf.tadami.extension.util.ExtensionsInstaller.Companion.EXTRA_DOWNLOAD_ID
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.preferences.extensions.ExtensionInstallerEnum

class ExtensionInstallService : Service() {

    private var installer: Installer? = null
    private lateinit var notifier : ExtensionInstallerNotifier

    override fun onCreate() {
        notifier = ExtensionInstallerNotifier(applicationContext)
        startForeground(Notifications.EXTENSIONS_INSTALLER_ID, notifier.updateNotification.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uri = intent?.data
        val id = intent?.getLongExtra(EXTRA_DOWNLOAD_ID, -1)?.takeIf { it != -1L }
        val installerUsed = intent?.getStringExtra(EXTRA_INSTALLER)
        if (uri == null || id == null || installerUsed == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (installer == null) {
            installer = when (ExtensionInstallerEnum.nullableValueOf(installerUsed)) {
                ExtensionInstallerEnum.PACKAGEINSTALLER -> TadamiPackageInstaller(this)
                else -> {
                    Log.d("ExtensionInstallService","Not implemented for installer $installerUsed")
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }
        installer!!.addToQueue(id, uri)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        installer?.onDestroy()
        installer = null
    }

    override fun onBind(i: Intent?): IBinder? = null

    companion object {
        private const val EXTRA_INSTALLER = "extra_extension_installer"

        fun getIntent(
            context: Context,
            downloadId: Long,
            uri: Uri,
            installer: ExtensionInstallerEnum,
        ): Intent {
            return Intent(context, ExtensionInstallService::class.java)
                .setDataAndType(uri, ExtensionsInstaller.APK_MIME)
                .putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                .putExtra(EXTRA_INSTALLER, installer.name)
        }
    }
}
