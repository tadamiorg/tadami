package com.sf.tadami.data.providers

import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import com.sf.tadami.R
import java.io.File

class AndroidFoldersProvider(
    private val context: Context,
)  {

    fun directory(): File {
        return File(
            Environment.getExternalStorageDirectory().absolutePath + File.separator + context.getString(R.string.app_name),
        )
    }

    fun path(): String {
        return directory().toUri().toString()
    }
}
