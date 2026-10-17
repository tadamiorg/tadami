package com.sf.tadami.data.download

import android.content.Context
import com.hippo.unifile.UniFile
import com.sf.tadami.domain.storage.StorageManager
import com.sf.tadami.source.Source
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Where the downloads are: `<storage>/downloads/<source>/<anime>/<episode>/`, see [DownloadNames]. */
class DownloadProvider(
    val context: Context,
    private val storageManager: StorageManager = Injekt.get(),
) {

    val downloadsDir: UniFile?
        get() = storageManager.getDownloadsDirectory()

    /**
     * Returns the download directory for a source if it exists.
     *
     * @param source the source to query.
     */
    fun findSourceDir(source: Source): UniFile? {
        return downloadsDir?.findFile(getSourceDirName(source))
    }

    fun findAnimeDir(source: Source, animeTitle: String): UniFile? {
        return findSourceDir(source)?.findFile(DownloadNames.animeDirName(animeTitle))
    }

    fun findEpisodeDir(source: Source, animeTitle: String, episodeDirName: String): UniFile? {
        return findAnimeDir(source, animeTitle)?.findFile(episodeDirName)
    }

    /** The anime directory, created when missing. Null when the storage can't be reached. */
    fun getOrCreateAnimeDir(source: Source, animeTitle: String): UniFile? {
        return downloadsDir
            ?.createDirectory(getSourceDirName(source))
            ?.createDirectory(DownloadNames.animeDirName(animeTitle))
    }

    /**
     * Returns the download directory name for a source.
     *
     * @param source the source to query.
     */
    fun getSourceDirName(source: Source): String {
        return DownloadNames.sourceDirName(source.id, source.name, source.lang)
    }
}
