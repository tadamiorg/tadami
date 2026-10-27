package com.sf.tadami.data.providers

import com.sf.tadami.utils.stringRes
import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import java.io.File

class AndroidFoldersProvider(
    private val context: Context,
)  {

    fun directory(): File {
        return File(
            Environment.getExternalStorageDirectory().absolutePath + File.separator + stringRes(Res.string.app_name),
        )
    }

    fun path(): String {
        return directory().toUri().toString()
    }
}
