package com.sf.tadami.ui.animeinfos.episode.cast.proxy

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.FileInputStream

/** Through the content resolver: a file descriptor, seekable, as the storage folder gives one. */
class ContentFileOpener(private val context: Context) : LocalFileOpener {
    override fun open(uri: String): FileInputStream? {
        val descriptor = context.contentResolver.openFileDescriptor(Uri.parse(uri), "r") ?: return null
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor)
    }
}
