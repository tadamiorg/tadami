package com.sf.tadami.data.download

import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.utils.DiskUtil
import com.sf.tadami.utils.Lang

/**
 * Names of the download directories and files, all valid for FAT and within
 * [DiskUtil.MAX_FILE_NAME_BYTES] UTF-8 bytes:
 * `<source>/<anime>/<episode>/<episode>.<ext>`, `<episode>.<lang>.<ext>` for the subtitles and
 * `<episode>.audio.<lang>.<ext>` for a separate audio.
 */
object DownloadNames {

    /** Kept free in an episode name for "_tmp", the collision suffix and the file suffixes */
    private const val EPISODE_SUFFIXES_BYTES = 64

    private const val AUDIO_PREFIX = "audio."
    private const val UNDETERMINED_LANG = "und"

    val SUBTITLE_EXTENSIONS = setOf("vtt", "srt", "ass", "ssa", "ttml", "dfxp")

    /**
     * Stable across app versions: the name and the language enum name, or the id when the source
     * is unknown. `toString()` isn't, it is `Object.toString()` for the extension sources.
     */
    fun sourceDirName(id: Long, name: String, lang: Lang): String {
        if (name.isBlank()) return id.toString()
        return validName("$name (${lang.name})", DiskUtil.MAX_FILE_NAME_BYTES)
    }

    fun animeDirName(title: String): String {
        return validName(title, DiskUtil.MAX_FILE_NAME_BYTES)
    }

    /**
     * The directory name of each episode of an anime, by episode id. Episodes whose names collide,
     * ignoring case as FAT and the FUSE storage do, get a suffix from their URL.
     */
    fun episodeDirNames(episodes: List<Episode>): Map<Long, String> {
        val bases = episodes.associate { it.id to episodeBaseName(it) }
        val collisions = bases.values.groupingBy { it.lowercase() }.eachCount()
        return episodes.associate { episode ->
            val base = bases.getValue(episode.id)
            val name = if (collisions.getValue(base.lowercase()) > 1) "$base (${urlHash(episode.url)})" else base
            episode.id to name
        }
    }

    fun episodeDirName(episode: Episode, episodes: List<Episode>): String {
        val all = if (episodes.any { it.id == episode.id }) episodes else episodes + episode
        return episodeDirNames(all).getValue(episode.id)
    }

    fun tmpDirName(episodeDirName: String): String = episodeDirName + TadamiDownloadManager.TMP_DIR_SUFFIX

    fun videoFileName(episodeDirName: String, extension: String) = "$episodeDirName.$extension"

    fun subtitleFileName(episodeDirName: String, lang: String, extension: String) =
        "$episodeDirName.$lang.$extension"

    fun audioFileName(episodeDirName: String, lang: String, extension: String) =
        "$episodeDirName.$AUDIO_PREFIX$lang.$extension"

    /** The audio file name without its extension, which is only known once the content is */
    fun audioFileBase(episodeDirName: String, lang: String) = "$episodeDirName.$AUDIO_PREFIX$lang"

    /**
     * Track languages usable in file names, unique ignoring case: the second "English" becomes
     * "English (2)".
     */
    fun trackNames(langs: List<String>): List<String> {
        val used = mutableSetOf<String>()
        return langs.map { lang ->
            val base = validName(lang.ifBlank { UNDETERMINED_LANG }, 32)
            var name = base
            var count = 1
            while (!used.add(name.lowercase())) {
                count++
                name = "$base ($count)"
            }
            name
        }
    }

    /** The files of a downloaded episode, from the names of its directory's files. */
    fun parseFiles(episodeDirName: String, fileNames: List<String>): EpisodeFiles {
        val prefix = "$episodeDirName."
        var video: String? = null
        val audios = mutableListOf<TrackFile>()
        val subtitles = mutableListOf<TrackFile>()
        fileNames.sorted().forEach { name ->
            if (!name.startsWith(prefix)) return@forEach
            val rest = name.removePrefix(prefix)
            val extension = rest.substringAfterLast('.')
            when {
                '.' !in rest -> if (video == null) video = name
                extension.lowercase() in SUBTITLE_EXTENSIONS ->
                    subtitles += TrackFile(name, rest.substringBeforeLast('.'), extension)
                rest.startsWith(AUDIO_PREFIX) ->
                    audios += TrackFile(name, rest.removePrefix(AUDIO_PREFIX).substringBeforeLast('.'), extension)
            }
        }
        return EpisodeFiles(video, audios, subtitles)
    }

    data class EpisodeFiles(
        val video: String?,
        val audios: List<TrackFile>,
        val subtitles: List<TrackFile>,
    )

    data class TrackFile(val name: String, val lang: String, val extension: String)

    private fun episodeBaseName(episode: Episode): String {
        val name = episode.name.ifBlank { "Episode ${formatNumber(episode.episodeNumber)}" }
        return validName(name, DiskUtil.MAX_FILE_NAME_BYTES - EPISODE_SUFFIXES_BYTES)
    }

    private fun formatNumber(number: Float): String {
        return if (number % 1f == 0f) number.toLong().toString() else number.toString()
    }

    // String.hashCode is specified, the same on every device and version
    private fun urlHash(url: String): String = "%08x".format(url.hashCode()).take(6)

    private fun validName(name: String, maxBytes: Int): String {
        val valid = truncateUtf8(DiskUtil.buildValidFilename(name), maxBytes).trimEnd('.', ' ')
        return valid.ifEmpty { "(invalid)" }
    }

    /** [name] cut to [maxBytes] UTF-8 bytes, without splitting a character. */
    internal fun truncateUtf8(name: String, maxBytes: Int): String {
        if (name.toByteArray(Charsets.UTF_8).size <= maxBytes) return name
        val result = StringBuilder()
        var bytes = 0
        var index = 0
        while (index < name.length) {
            val codePoint = name.codePointAt(index)
            val size = String(Character.toChars(codePoint)).toByteArray(Charsets.UTF_8).size
            if (bytes + size > maxBytes) break
            result.appendCodePoint(codePoint)
            bytes += size
            index += Character.charCount(codePoint)
        }
        return result.toString()
    }
}
