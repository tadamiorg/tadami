package com.sf.tadami.data.download.stream

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.UriUtil
import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistParser
import com.hippo.unifile.UniFile
import com.sf.tadami.data.download.FileProgress
import com.sf.tadami.network.player.HlsSegmentIvs
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Headers
import java.io.IOException
import java.io.OutputStream
import java.security.GeneralSecurityException

/**
 * One HLS rendition joined into one file, without ffmpeg: MPEG-TS segments concatenated into a
 * `.ts`, fMP4 segments after their init segment into a `.mp4`. A few segments are fetched in
 * parallel and written in order, so a resume only needs the count of written segments.
 */
@OptIn(UnstableApi::class)
class HlsDownload(private val context: Context, private val http: MediaHttp) {

    /** The separate audio of the chosen variant, to download next to the video. */
    class AudioRendition(val url: String, val lang: String)

    /** Where a resumed download goes on: after [segments] segments, [bytes] long. */
    data class ResumePoint(val segments: Int, val bytes: Long)

    private class Segment(val url: String, val range: String?, val keyUrl: String?, val iv: ByteArray?)

    /**
     * Downloads the playlist at [url] into [dir] as `<fileBase>.<ext>`. [playlist] is its content
     * when already read, [url] then being where it was read from. For a multivariant playlist, the
     * variant closest to [quality].
     */
    suspend fun download(
        url: String,
        playlist: String?,
        headers: Headers?,
        dir: UniFile,
        fileBase: String,
        quality: String,
        audio: Boolean,
        resume: FileProgress?,
        onAudioRendition: (AudioRendition) -> Unit,
        onFileProgress: (FileProgress) -> Unit,
        onProgress: (Float) -> Unit,
    ): UniFile {
        val (baseUrl, text) = if (playlist != null) url to playlist else fetchPlaylist(url, headers)
        var parsed = parse(baseUrl, text)
        if (parsed is HlsMultivariantPlaylist) {
            val variant = chooseVariant(parsed, quality) ?: throw StreamFailedException("No variant in the playlist")
            audioRenditionOf(parsed, variant)?.let(onAudioRendition)
            val (mediaUrl, mediaText) = fetchPlaylist(variant.url.toString(), headers)
            parsed = parse(mediaUrl, mediaText)
        }
        val media = parsed as? HlsMediaPlaylist ?: throw StreamFailedException("No media playlist")
        checkSupported(media)
        if (!audio && MediaCheck.isTooShort(media.durationUs / 1000)) {
            throw StreamFailedException("The video lasts ${media.durationUs / 1_000_000} s only")
        }

        val segments = media.segments
            .withIndex()
            // A gap has no data
            .filterNot { (_, segment) -> segment.hasGapTag }
            .map { (index, segment) -> segmentOf(media.baseUri, segment, media.mediaSequence + index) }
        val init = media.segments.firstNotNullOfOrNull { it.initializationSegment }
            ?.let { segmentOf(media.baseUri, it, media.mediaSequence) }
        return writeSegments(segments, init, headers, dir, fileBase, audio, resume, onFileProgress, onProgress)
    }

    /** Where the playlist was read from, after the redirects, and its content */
    private suspend fun fetchPlaylist(url: String, headers: Headers?): Pair<String, String> {
        return http.read(url, headers) { response ->
            // Relative URIs are relative to where a redirect led
            response.request.url.toString() to response.body.string()
        }
    }

    private fun parse(url: String, text: String): HlsPlaylist {
        if (!MediaCheck.isPlaylist(text.take(MediaCheck.SNIFF_BYTES).encodeToByteArray())) {
            throw StreamFailedException("Not an HLS playlist")
        }
        // Kwik's segments keep their encoder number as IV after a gap
        val fixed = HlsSegmentIvs.fix(text) ?: text
        return try {
            HlsPlaylistParser().parse(Uri.parse(url), fixed.byteInputStream())
        } catch (e: IOException) {
            throw StreamFailedException("Unreadable playlist: ${e.message}", e)
        }
    }

    private fun chooseVariant(playlist: HlsMultivariantPlaylist, quality: String): HlsMultivariantPlaylist.Variant? {
        val variants = playlist.variants.mapIndexed { index, variant ->
            HlsVariants.Variant(
                index = index,
                height = variant.format.height.coerceAtLeast(0),
                bandwidth = variant.format.bitrate.coerceAtLeast(0),
                demuxedAudio = audioRenditions(playlist, variant).isNotEmpty(),
            )
        }
        return HlsVariants.chooseVariant(variants, quality)?.let { playlist.variants[it.index] }
    }

    // Renditions without a URI are the muxed audio, described
    private fun audioRenditions(
        playlist: HlsMultivariantPlaylist,
        variant: HlsMultivariantPlaylist.Variant,
    ): List<IndexedValue<HlsMultivariantPlaylist.Rendition>> {
        val group = variant.audioGroupId ?: return emptyList()
        return playlist.audios.withIndex().filter { (_, rendition) -> rendition.groupId == group && rendition.url != null }
    }

    private fun audioRenditionOf(
        playlist: HlsMultivariantPlaylist,
        variant: HlsMultivariantPlaylist.Variant,
    ): AudioRendition? {
        val renditions = audioRenditions(playlist, variant).map { (index, rendition) ->
            HlsVariants.AudioRendition(index, rendition.format.selectionFlags and C.SELECTION_FLAG_DEFAULT != 0)
        }
        val chosen = playlist.audios[HlsVariants.chooseAudio(renditions)?.index ?: return null]
        return AudioRendition(chosen.url.toString(), chosen.format.language ?: chosen.name)
    }

    private fun checkSupported(media: HlsMediaPlaylist) {
        if (!media.hasEndTag) throw StreamFailedException("Live playlist")
        if (media.segments.isEmpty()) throw StreamFailedException("Empty playlist")
        if (media.protectionSchemes != null || media.segments.any { it.drmInitData != null }) {
            throw StreamFailedException("DRM protected playlist")
        }
        val inits = media.segments.mapTo(mutableSetOf()) { segment ->
            segment.initializationSegment?.let { "${it.url}|${it.byteRangeOffset}|${it.byteRangeLength}" }
        }
        // A second init segment would need a second file, players don't read joined ones
        if (inits.size > 1) throw StreamFailedException("Several init segments")
    }

    private fun segmentOf(baseUri: String, segment: HlsMediaPlaylist.SegmentBase, mediaSequence: Long): Segment {
        val range = if (segment.byteRangeLength != C.LENGTH_UNSET.toLong()) {
            "bytes=${segment.byteRangeOffset}-${segment.byteRangeOffset + segment.byteRangeLength - 1}"
        } else {
            null
        }
        val keyUrl = segment.fullSegmentEncryptionKeyUri?.let { UriUtil.resolve(baseUri, it) }
        return Segment(
            url = UriUtil.resolve(baseUri, segment.url),
            range = range,
            keyUrl = keyUrl,
            iv = keyUrl?.let { HlsCrypto.iv(segment.encryptionIV, mediaSequence) },
        )
    }

    private suspend fun writeSegments(
        segments: List<Segment>,
        init: Segment?,
        headers: Headers?,
        dir: UniFile,
        fileBase: String,
        audio: Boolean,
        resume: FileProgress?,
        onFileProgress: (FileProgress) -> Unit,
        onProgress: (Float) -> Unit,
    ): UniFile = coroutineScope {
        val count = segments.size
        val keys = Keys(headers)
        var file = resume?.takeIf { it.hls }?.let { dir.findFile(it.name) }
        val point = resumePoint(resume, count, file?.length())
        if (point == null) {
            file?.delete()
            file = null
        }
        var output: OutputStream? = file?.let { DownloadOutput.openAt(context, it, point!!.bytes) }
        var written = point?.bytes ?: 0L
        var done = point?.segments ?: 0
        try {
            val window = ArrayDeque<Deferred<ByteArray>>()
            var next = done
            fun fetchNext() {
                val segment = segments[next++]
                window.addLast(async { fetch(segment, headers, keys) })
            }
            repeat(minOf(PARALLEL_SEGMENTS, count - done)) { fetchNext() }
            while (done < count) {
                val data = window.removeFirst().await()
                if (next < count) fetchNext()
                var target = file
                if (target == null || output == null) {
                    // The first segment tells the container
                    val extension = MediaCheck.hlsExtension(init != null, audio, data)
                    target = DownloadOutput.getOrCreateFile(dir, "$fileBase.$extension")
                    file = target
                    output = DownloadOutput.openAt(context, target, 0)
                    onFileProgress(FileProgress(name = target.name.orEmpty(), hls = true, total = count.toLong()))
                    if (init != null) {
                        val initData = fetch(init, headers, keys)
                        DownloadOutput.write(output, initData)
                        written += initData.size
                    }
                }
                DownloadOutput.write(output, data)
                written += data.size
                done++
                onFileProgress(
                    FileProgress(name = target.name.orEmpty(), hls = true, bytes = written, segments = done, total = count.toLong()),
                )
                onProgress(done.toFloat() / count)
            }
            file ?: throw StreamFailedException("Nothing downloaded")
        } finally {
            output?.close()
        }
    }

    private suspend fun fetch(segment: Segment, headers: Headers?, keys: Keys): ByteArray {
        val data = http.bytes(segment.url, headers, segment.range)
        if (data.isEmpty()) return data
        if (segment.keyUrl == null) {
            if (MediaCheck.looksLikeText(data.copyOf(minOf(data.size, MediaCheck.SNIFF_BYTES)))) {
                throw StreamFailedException("A segment is a text or HTML page")
            }
            return data
        }
        return try {
            HlsCrypto.decrypt(data, keys.get(segment.keyUrl), segment.iv!!)
        } catch (e: GeneralSecurityException) {
            throw StreamFailedException("Can't decrypt a segment", e)
        }
    }

    /** The AES keys, fetched once per URI with the stream's headers. */
    private inner class Keys(private val headers: Headers?) {
        private val mutex = Mutex()
        private val keys = mutableMapOf<String, ByteArray>()

        suspend fun get(url: String): ByteArray = mutex.withLock {
            keys[url] ?: http.bytes(url, headers).also { key ->
                if (key.size != KEY_SIZE) throw StreamFailedException("AES-128 key of ${key.size} bytes")
                keys[url] = key
            }
        }
    }

    companion object {
        private const val PARALLEL_SEGMENTS = 3
        private const val KEY_SIZE = 16

        /**
         * Where to resume: after the segments written in order, the file cut to their length (a
         * segment may have been half written). Null to start over: no data, another playlist, or a
         * file shorter than saved.
         */
        fun resumePoint(resume: FileProgress?, segmentCount: Int, fileLength: Long?): ResumePoint? {
            if (resume == null || !resume.hls || resume.segments <= 0) return null
            if (resume.total != segmentCount.toLong() || resume.segments > segmentCount) return null
            if (fileLength == null || fileLength < resume.bytes) return null
            return ResumePoint(resume.segments, resume.bytes)
        }
    }
}
