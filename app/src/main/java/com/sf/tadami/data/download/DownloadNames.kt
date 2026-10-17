package com.sf.tadami.data.download

import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.utils.DiskUtil
import com.sf.tadami.utils.Lang
import java.security.MessageDigest

/**
 * Names of the download directories and files, all valid for FAT and within
 * [DiskUtil.MAX_FILE_NAME_BYTES] UTF-8 bytes:
 * `<source>/<anime>/<episode>/<episode>.<ext>`, `<episode>.<lang>.<ext>` for the subtitles,
 * `<episode>.audio.<lang>.<ext>` for a separate audio and `<episode>.json` for the sidecar.
 */
object DownloadNames {

    /** Kept free in an episode name for "_tmp", the key and the file suffixes */
    private const val EPISODE_SUFFIXES_BYTES = 64

    /** " (<6 hex chars>)" */
    private const val ANIME_KEY_BYTES = 9
    private val ANIME_KEY_REGEX = Regex(""" \(([0-9a-f]{6})\)$""")

    private const val EPISODE_KEY_SIZE = 4
    private val EPISODE_KEY_REGEX = Regex(""" \(([0-9a-f]{8})\)$""")

    private const val AUDIO_PREFIX = "audio."
    private const val METADATA_EXTENSION = "json"
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

    /**
     * `<title> (<key>)`, the key from the anime URL: two anime of one source with the same title get
     * their own folders, and the folder is found again by its key when the title changes. Always
     * suffixed, not only on a collision: the anime in the database change as the user browses, a
     * suffix added later would orphan the folder.
     */
    fun animeDirName(title: String, url: String): String {
        return "${validName(title, DiskUtil.MAX_FILE_NAME_BYTES - ANIME_KEY_BYTES)} (${animeKey(url)})"
    }

    fun animeKey(url: String): String = urlHash(url, 3)

    /** The key of an anime directory name, null when it has none. */
    fun animeKeyOf(dirName: String): String? = ANIME_KEY_REGEX.find(dirName)?.groupValues?.get(1)

    /**
     * The directory of an anime among [dirNames], one source's: [expected] itself, else the one with
     * its key, named after an older title. Null when there is none.
     */
    fun findAnimeDirName(dirNames: Collection<String>, expected: String): String? {
        if (expected in dirNames) return expected
        val key = animeKeyOf(expected) ?: return null
        return dirNames.firstOrNull { animeKeyOf(it) == key }
    }

    /**
     * `<episode name> (<key>)`, the key from the episode URL: episodes with the same name get their
     * own folders, and the folder is found again by its key when the source renames the episode.
     */
    fun episodeDirName(episode: Episode): String = "${episodeBaseName(episode)} (${episodeKey(episode.url)})"

    /** 4 bytes, not the anime's 3: one anime can have a thousand episodes, all looked up by key. */
    fun episodeKey(url: String): String = urlHash(url, EPISODE_KEY_SIZE)

    /** The key of an episode directory name, null when it has none. */
    fun episodeKeyOf(dirName: String): String? = EPISODE_KEY_REGEX.find(dirName)?.groupValues?.get(1)

    /**
     * The episode directories of one anime, by their names. An episode's is the one with its name,
     * else the one with its key, named after an older episode name, else one of the earlier,
     * unreleased layout: the bare name, or the name with a 6 chars collision suffix.
     */
    class EpisodeDirs(dirNames: Collection<String>) {
        private val names = dirNames.toSet()
        private val byKey = names.mapNotNull { name -> episodeKeyOf(name)?.let { it to name } }.toMap()

        fun find(episode: Episode): String? {
            val expected = episodeDirName(episode)
            if (expected in names) return expected
            byKey[episodeKey(episode.url)]?.let { return it }
            val base = episodeBaseName(episode)
            return base.takeIf { it in names } ?: "$base (${urlHash(episode.url, 3)})".takeIf { it in names }
        }
    }

    fun tmpDirName(episodeDirName: String): String = episodeDirName + TadamiDownloadManager.TMP_DIR_SUFFIX

    fun videoFileName(episodeDirName: String, extension: String) = "$episodeDirName.$extension"

    fun subtitleFileName(episodeDirName: String, lang: String, extension: String) =
        "$episodeDirName.$lang.$extension"

    fun audioFileName(episodeDirName: String, lang: String, extension: String) =
        "$episodeDirName.$AUDIO_PREFIX$lang.$extension"

    /** The sidecar, see [EpisodeMetadata] */
    fun metadataFileName(episodeDirName: String) = "$episodeDirName.$METADATA_EXTENSION"

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
        val metadataName = metadataFileName(episodeDirName)
        var video: String? = null
        var metadata: String? = null
        val audios = mutableListOf<TrackFile>()
        val subtitles = mutableListOf<TrackFile>()
        fileNames.sorted().forEach { name ->
            if (!name.startsWith(prefix)) return@forEach
            val rest = name.removePrefix(prefix)
            val extension = rest.substringAfterLast('.')
            when {
                // Not a video, though named like one
                name == metadataName -> metadata = name
                '.' !in rest -> if (video == null) video = name
                extension.lowercase() in SUBTITLE_EXTENSIONS ->
                    subtitles += TrackFile(name, rest.substringBeforeLast('.'), extension)
                rest.startsWith(AUDIO_PREFIX) ->
                    audios += TrackFile(name, rest.removePrefix(AUDIO_PREFIX).substringBeforeLast('.'), extension)
            }
        }
        return EpisodeFiles(video, audios, subtitles, metadata)
    }

    data class EpisodeFiles(
        val video: String?,
        val audios: List<TrackFile>,
        val subtitles: List<TrackFile>,
        /** The sidecar, none for the downloads made before it */
        val metadata: String? = null,
    )

    data class TrackFile(val name: String, val lang: String, val extension: String)

    private fun episodeBaseName(episode: Episode): String {
        val name = episode.name.ifBlank { "Episode ${formatNumber(episode.episodeNumber)}" }
        return validName(name, DiskUtil.MAX_FILE_NAME_BYTES - EPISODE_SUFFIXES_BYTES)
    }

    private fun formatNumber(number: Float): String {
        return if (number % 1f == 0f) number.toLong().toString() else number.toString()
    }

    /**
     * The first [bytes] bytes of the URL's SHA-256, in hex: the same on every device and version. Not
     * the first chars of `String.hashCode`, which stay the same for URLs that differ only at the end
     * ("/ep/1", "/ep/2").
     */
    private fun urlHash(url: String, bytes: Int): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray(Charsets.UTF_8))
        return digest.take(bytes).joinToString("") { "%02x".format(it) }
    }

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
