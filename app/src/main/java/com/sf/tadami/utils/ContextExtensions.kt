package com.sf.tadami.utils

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.PermissionChecker
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.work.WorkManager
import com.sf.tadami.ui.utils.UiToasts
import java.io.File

fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("no activity")
}

@ColorInt
fun Context.getColorFromAttr( @AttrRes attrColor: Int
): Int {
    val typedArray = theme.obtainStyledAttributes(intArrayOf(attrColor))
    val textColor = typedArray.getColor(0, 0)
    typedArray.recycle()
    return textColor
}

fun Context.createFileInCacheDir(name: String): File {
    val file = File(externalCacheDir, name)
    if (file.exists()) {
        file.delete()
    }
    file.createNewFile()
    return file
}

fun Context.notify(notificationId : Int,notification : Notification){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && PermissionChecker.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PermissionChecker.PERMISSION_GRANTED) {
        return
    }
    NotificationManagerCompat.from(this).notify(notificationId,notification)
}

fun Context.cancelNotification(id: Int) {
    NotificationManagerCompat.from(this).cancel(id)
}

val Context.animatorDurationScale: Float
    get() = Settings.Global.getFloat(this.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

val Context.notificationManager: NotificationManager
    get() = getSystemService()!!

val Context.powerManager: PowerManager
    get() = getSystemService()!!

val Context.workManager: WorkManager
    get() = WorkManager.getInstance(this)

fun Context.hasPermission(
    permission: String,
) = PermissionChecker.checkSelfPermission(this, permission) == PermissionChecker.PERMISSION_GRANTED

val Context.wifiManager: WifiManager
    get() = getSystemService()!!

val Context.connectivityManager: ConnectivityManager
    get() = getSystemService()!!
fun Context.isConnectedToWifi(): Boolean {
    if (!wifiManager.isWifiEnabled) return false

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } else {
        @Suppress("DEPRECATION")
        wifiManager.connectionInfo.bssid != null
    }
}

fun Context.openInBrowser(url: String, forceDefaultBrowser: Boolean = false) {
    this.openInBrowser(url.toUri(), forceDefaultBrowser)
}

fun Context.openInBrowser(uri: Uri, forceDefaultBrowser: Boolean = false) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            // Force default browser so that verified extensions don't re-open Tadami
            if (forceDefaultBrowser) {
                defaultBrowserPackageName()?.let { setPackage(it) }
            }
        }
        startActivity(intent)
    } catch (e: Exception) {
        UiToasts.showToast(e.message ?: "")
    }
}

private fun Context.defaultBrowserPackageName(): String? {
    val browserIntent = Intent(Intent.ACTION_VIEW, "http://".toUri())
    val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.resolveActivity(
            browserIntent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
        )
    } else {
        packageManager.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
    }
    return resolveInfo
        ?.activityInfo?.packageName
        ?.takeUnless { it in DeviceUtil.invalidDefaultBrowsers }
}
