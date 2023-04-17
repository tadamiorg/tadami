package com.sf.tadami.ui.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.sf.tadami.BuildConfig
import java.io.File

fun File.getUriCompat(context: Context): Uri {
    return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", this)
}