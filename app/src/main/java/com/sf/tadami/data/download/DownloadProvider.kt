package com.sf.tadami.data.download

import android.content.Context
import com.hippo.unifile.UniFile
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
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

    /** The anime directory if it exists, found by its key when it is named after an older title. */
    fun findAnimeDir(source: Source, anime: Anime): UniFile? {
        val dirs = findSourceDir(source)?.listFiles() ?: return null
        val name = DownloadNames.findAnimeDirName(dirs.mapNotNull { it.name }, getAnimeDirName(anime))
            ?: return null
        return dirs.find { it.name == name }
    }

    /** The episode directory if it exists, found by its key when it is named after an older name. */
    fun findEpisodeDir(source: Source, anime: Anime, episode: Episode): UniFile? {
        val dirs = findAnimeDir(source, anime)?.listFiles()?.filter { it.isDirectory } ?: return null
        val name = DownloadNames.EpisodeDirs(dirs.mapNotNull { it.name }).find(episode) ?: return null
        return dirs.find { it.name == name }
    }

    /** The anime directory, created when missing. Null when the storage can't be reached. */
    fun getOrCreateAnimeDir(source: Source, anime: Anime): UniFile? {
        return findAnimeDir(source, anime)
            ?: downloadsDir
                ?.createDirectory(getSourceDirName(source))
                ?.createDirectory(getAnimeDirName(anime))
    }

    /**
     * Returns the download directory name for a source.
     *
     * @param source the source to query.
     */
    fun getSourceDirName(source: Source): String {
        return DownloadNames.sourceDirName(source.id, source.name, source.lang)
    }

    /** The name a new directory of [anime] gets, see [DownloadNames.animeDirName]. */
    fun getAnimeDirName(anime: Anime): String {
        return DownloadNames.animeDirName(anime.title, anime.url)
    }
}
