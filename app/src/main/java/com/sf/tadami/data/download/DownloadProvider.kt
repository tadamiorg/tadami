package com.sf.tadami.data.download

import android.content.Context
import com.hippo.unifile.UniFile
import com.sf.tadami.domain.storage.StorageManager
import com.sf.tadami.source.Source
import com.sf.tadami.utils.DiskUtil.buildValidFilename
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DownloadProvider(
    val context: Context,
    private val storageManager: StorageManager = Injekt.get(),
) {

    private val downloadsDir: UniFile?
        get() = storageManager.getDownloadsDirectory()

    /**
     * Returns the download directory for a source if it exists.
     *
     * @param source the source to query.
     */
    fun findSourceDir(source: Source): UniFile? {
        return downloadsDir?.findFile(getSourceDirName(source), true)
    }

    /**
     * Returns the download directory name for a source.
     *
     * @param source the source to query.
     */
    fun getSourceDirName(source: Source): String {
        return buildValidFilename(source.toString())
    }
}