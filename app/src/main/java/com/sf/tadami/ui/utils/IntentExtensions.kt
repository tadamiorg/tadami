package com.sf.tadami.ui.utils

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.sf.tadami.R

fun Uri.toShareIntent(context: Context, type: String = "image/*", message: String? = null): Intent {
    val uri = this

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        when (uri.scheme) {
            "http", "https" -> {
                putExtra(Intent.EXTRA_TEXT, uri.toString())
            }
            "content" -> {
                message?.let { putExtra(Intent.EXTRA_TEXT, it) }
                putExtra(Intent.EXTRA_STREAM, uri)
            }
        }
        clipData = ClipData.newRawUri(null, uri)
        setType(type)
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

    // Added, not set: createChooser gives the chooser the read grant of the share intent, for
    // its preview
    return Intent.createChooser(shareIntent, context.getString(R.string.action_share)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
