package com.sf.tadami.ui.animeinfos.episode.cast.proxy

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.sf.tadami.data.download.EpisodePreviews
import com.sf.tadami.data.download.SegmentMetadata
import fi.iki.elonen.NanoHTTPD
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * What the proxy serves of the downloaded episode being cast, by name: its files, read through
 * their content URIs, and playlists over them. [key] tells one episode's files from another's.
 * [previews], its seekbar preview frames, aren't served: the cast's seekbar preview reads them.
 */
data class LocalMedia(
    val key: String,
    val files: Map<String, LocalFile>,
    val playlists: Map<String, LocalPlaylist> = emptyMap(),
    val previews: EpisodePreviews? = null,
)

data class LocalFile(val uri: String, val mimeType: String)

sealed interface LocalPlaylist {
    /** The HLS segments joined in the file named [file], as byte ranges of it */
    data class Media(val file: String, val segments: List<SegmentMetadata>) : LocalPlaylist

    /** The video playlist [video] with the audio playlists [audios] (name to language) as renditions */
    data class Master(val video: String, val audios: List<Pair<String, String>>, val bandwidth: Long) : LocalPlaylist
}

/** Whether [url] is one of the local route's: downloaded files the proxy serves during a cast only. */
fun isLocalMediaUrl(url: String): Boolean =
    url.toHttpUrlOrNull()?.encodedPath?.startsWith(LocalMediaRoute.PATH_PREFIX) == true

/** Opens a downloaded file for reading, null when it can't be. */
fun interface LocalFileOpener {
    fun open(uri: String): FileInputStream?
}

/** Through the content resolver: a file descriptor, seekable, as the storage folder gives one. */
class ContentFileOpener(private val context: Context) : LocalFileOpener {
    override fun open(uri: String): FileInputStream? {
        val descriptor = context.contentResolver.openFileDescriptor(Uri.parse(uri), "r") ?: return null
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor)
    }
}

/** HLS playlists over the joined files of a download, URIs relative to the playlist's own URL. */
object LocalPlaylists {

    const val MIME_TYPE = "application/vnd.apple.mpegurl"

    private const val AUDIO_GROUP = "audio"

    /** A VOD media playlist of [segments], byte ranges of [uri]. */
    fun media(uri: String, segments: List<SegmentMetadata>): String {
        val maxDurationUs = segments.maxOfOrNull { it.durationUs } ?: 0L
        return buildString {
            appendLine("#EXTM3U")
            // EXT-X-BYTERANGE needs 4
            appendLine("#EXT-X-VERSION:4")
            // Rounded up: no segment may last longer
            appendLine("#EXT-X-TARGETDURATION:${((maxDurationUs + 999_999) / 1_000_000).coerceAtLeast(1)}")
            appendLine("#EXT-X-MEDIA-SEQUENCE:0")
            appendLine("#EXT-X-PLAYLIST-TYPE:VOD")
            segments.forEach { segment ->
                if (segment.discontinuity) appendLine("#EXT-X-DISCONTINUITY")
                appendLine("#EXTINF:${seconds(segment.durationUs)},")
                // A segment the server sent empty: no bytes to ask for, its time kept
                if (segment.length > 0) {
                    appendLine("#EXT-X-BYTERANGE:${segment.length}@${segment.offset}")
                } else {
                    appendLine("#EXT-X-GAP")
                }
                appendLine(uri)
            }
            appendLine("#EXT-X-ENDLIST")
        }
    }

    /** A master playlist: the video playlist [videoUri], each of [audios] (URI to name) an audio rendition. */
    fun master(videoUri: String, bandwidth: Long, audios: List<Pair<String, String>>): String {
        return buildString {
            appendLine("#EXTM3U")
            appendLine("#EXT-X-VERSION:4")
            audios.forEachIndexed { index, (uri, name) ->
                appendLine(
                    "#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"$AUDIO_GROUP\",NAME=\"${name.replace("\"", "")}\"," +
                        "DEFAULT=${if (index == 0) "YES" else "NO"},AUTOSELECT=YES,URI=\"$uri\"",
                )
            }
            val audioGroup = if (audios.isNotEmpty()) ",AUDIO=\"$AUDIO_GROUP\"" else ""
            appendLine("#EXT-X-STREAM-INF:BANDWIDTH=${bandwidth.coerceAtLeast(1)}$audioGroup")
            appendLine(videoUri)
        }
    }

    /** The highest bit rate of one of [segments], in bits per second: a variant's BANDWIDTH. */
    fun peakBitrate(segments: List<SegmentMetadata>): Long {
        return segments.filter { it.durationUs > 0 }.maxOfOrNull { it.length * 8 * 1_000_000 / it.durationUs } ?: 0L
    }

    // Exact, no floating point: 10010000 -> "10.010000"
    private fun seconds(durationUs: Long): String {
        return "${durationUs / 1_000_000}.${(durationUs % 1_000_000).toString().padStart(6, '0')}"
    }
}

/** The part of a file a `Range` header asks for. */
sealed interface ByteRange {
    /** No range, or one that is ignored (another unit, several ranges, malformed): the whole file */
    data object Whole : ByteRange

    data class Part(val start: Long, val end: Long) : ByteRange {
        val length: Long get() = end - start + 1
    }

    /** Starts past the end of the file: 416 */
    data object Unsatisfiable : ByteRange

    companion object {
        /** The range [header] asks for in a file of [length] bytes. */
        fun parse(header: String?, length: Long): ByteRange {
            val spec = header?.trim()?.takeIf { it.startsWith("bytes=", ignoreCase = true) }
                ?.substring("bytes=".length)?.trim()
                ?: return Whole
            if (',' in spec || '-' !in spec) return Whole
            val first = spec.substringBefore('-').trim()
            val last = spec.substringAfter('-').trim()
            if (first.isEmpty()) {
                // The last bytes
                val suffix = last.toLongOrNull()?.takeIf { it >= 0 } ?: return Whole
                if (suffix == 0L || length == 0L) return Unsatisfiable
                return Part((length - suffix).coerceAtLeast(0), length - 1)
            }
            val start = first.toLongOrNull()?.takeIf { it >= 0 } ?: return Whole
            val end = if (last.isEmpty()) length - 1 else last.toLongOrNull()?.takeIf { it >= start } ?: return Whole
            if (start >= length) return Unsatisfiable
            return Part(start, minOf(end, length - 1))
        }
    }
}

/**
 * The route serving the downloaded episode being cast: `/local-<token>-<name>`, `<name>` one of
 * [LocalMedia]'s, never a path. The token is random (128 bits), renewed for each other episode,
 * gone when the proxy stops: other devices on the network can't guess it, and nothing but the
 * files of the episode cast is reachable with it.
 */
internal class LocalMediaRoute(private val opener: LocalFileOpener) {

    private class Served(val token: String, val media: LocalMedia)

    @Volatile
    private var served: Served? = null

    private val random = SecureRandom()

    /** Serves [media] instead of what was served: the path prefix of its URLs, `/local-<token>-`. */
    @Synchronized
    fun serve(media: LocalMedia): String {
        // The same URLs for the same episode: the receiver tells a reload by its URL
        val token = served?.takeIf { it.media.key == media.key }?.token ?: newToken()
        served = Served(token, media)
        return "$PATH_PREFIX$token-"
    }

    @Synchronized
    fun clear() {
        served = null
    }

    fun handles(path: String): Boolean = path.startsWith(PATH_PREFIX)

    /**
     * The content URI of the file behind [path], a URL path of the episode served: the file itself,
     * the joined file of a media playlist, or the video's of the master playlist. Null for anything
     * else (another token, another name).
     */
    fun fileUri(path: String): String? {
        val current = served ?: return null
        val rest = path.removePrefix(PATH_PREFIX).takeIf { path.startsWith(PATH_PREFIX) } ?: return null
        if (!sameToken(rest.substringBefore('-', ""), current.token)) return null
        var name = rest.substringAfter('-')
        (current.media.playlists[name] as? LocalPlaylist.Master)?.let { name = it.video }
        (current.media.playlists[name] as? LocalPlaylist.Media)?.let { name = it.file }
        return current.media.files[name]?.uri
    }

    /** The seekbar preview frames of the episode served, when [path] is one of its URL paths; null otherwise. */
    fun previews(path: String): EpisodePreviews? {
        val current = served ?: return null
        val rest = path.removePrefix(PATH_PREFIX).takeIf { path.startsWith(PATH_PREFIX) } ?: return null
        if (!sameToken(rest.substringBefore('-', ""), current.token)) return null
        return current.media.previews
    }

    fun respond(method: NanoHTTPD.Method, path: String, rangeHeader: String?): NanoHTTPD.Response {
        val current = served
        val rest = path.removePrefix(PATH_PREFIX)
        val token = rest.substringBefore('-', "")
        if (current == null || !sameToken(token, current.token)) return text(NanoHTTPD.Response.Status.FORBIDDEN, "forbidden")
        val name = rest.substringAfter('-')
        val head = method == NanoHTTPD.Method.HEAD
        current.media.playlists[name]?.let { playlist ->
            val bytes = playlistText(playlist, "${PATH_PREFIX.removePrefix("/")}${current.token}-").toByteArray(Charsets.UTF_8)
            return response(NanoHTTPD.Response.Status.OK, LocalPlaylists.MIME_TYPE, if (head) ByteArray(0) else bytes, bytes.size.toLong())
        }
        val file = current.media.files[name] ?: return text(NanoHTTPD.Response.Status.NOT_FOUND, "not found")
        return respondFile(file, head, rangeHeader)
    }

    private fun respondFile(file: LocalFile, head: Boolean, rangeHeader: String?): NanoHTTPD.Response {
        val input = try {
            opener.open(file.uri)
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        } ?: return text(NanoHTTPD.Response.Status.NOT_FOUND, "not found")
        try {
            val length = input.channel.size()
            val response = when (val range = ByteRange.parse(rangeHeader, length)) {
                ByteRange.Whole -> {
                    if (head) input.close()
                    LocalResponse(NanoHTTPD.Response.Status.OK, file.mimeType, if (head) empty() else input, length)
                }
                is ByteRange.Part -> {
                    if (head) input.close() else input.channel.position(range.start)
                    LocalResponse(NanoHTTPD.Response.Status.PARTIAL_CONTENT, file.mimeType, if (head) empty() else input, range.length)
                        .apply { addHeader("Content-Range", "bytes ${range.start}-${range.end}/$length") }
                }
                ByteRange.Unsatisfiable -> {
                    input.close()
                    text(NanoHTTPD.Response.Status.RANGE_NOT_SATISFIABLE, "range not satisfiable")
                        .apply { addHeader("Content-Range", "bytes */$length") }
                }
            }
            return response.apply { addHeader("Accept-Ranges", "bytes") }
        } catch (e: IOException) {
            input.close()
            throw e
        }
    }

    private fun playlistText(playlist: LocalPlaylist, namePrefix: String): String {
        return when (playlist) {
            is LocalPlaylist.Media -> LocalPlaylists.media(namePrefix + playlist.file, playlist.segments)
            is LocalPlaylist.Master -> LocalPlaylists.master(
                videoUri = namePrefix + playlist.video,
                bandwidth = playlist.bandwidth,
                audios = playlist.audios.map { (name, lang) -> namePrefix + name to lang },
            )
        }
    }

    private fun newToken(): String {
        val bytes = ByteArray(TOKEN_BYTES).also(random::nextBytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // Constant time: how much of a guess was right doesn't show
    private fun sameToken(given: String, token: String): Boolean =
        MessageDigest.isEqual(given.toByteArray(Charsets.US_ASCII), token.toByteArray(Charsets.US_ASCII))

    private fun empty(): InputStream = ByteArrayInputStream(ByteArray(0))

    private fun response(status: NanoHTTPD.Response.IStatus, mime: String, body: ByteArray, length: Long) =
        LocalResponse(status, mime, ByteArrayInputStream(body), length)

    private fun text(status: NanoHTTPD.Response.IStatus, text: String): NanoHTTPD.Response {
        val bytes = text.toByteArray(Charsets.UTF_8)
        return response(status, "text/plain", bytes, bytes.size.toLong())
    }

    companion object {
        const val PATH_PREFIX = "/local-"
        private const val TOKEN_BYTES = 16
    }
}

/**
 * A response of the local route, never gzipped by NanoHTTPD: its length is the file's or the
 * range's, and a gzipped HEAD would carry a body.
 */
internal class LocalResponse(status: NanoHTTPD.Response.IStatus, mimeType: String, data: InputStream, length: Long) :
    NanoHTTPD.Response(status, mimeType, data, length)
