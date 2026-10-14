package com.sf.tadami.crash

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.sf.tadami.BuildConfig
import java.io.File

/**
 * Gives the crash report the crash screen shares, from the crash screen's own process: reading it
 * from the app's provider, which runs in the main process, would start that process again, and
 * load the extensions, maybe the one that crashed.
 */
class CrashReportProvider : FileProvider() {

    companion object {
        private const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.crashreport"

        fun getUri(context: Context, file: File): Uri = getUriForFile(context, AUTHORITY, file)
    }
}
