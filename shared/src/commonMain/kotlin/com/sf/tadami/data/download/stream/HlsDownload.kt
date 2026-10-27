package com.sf.tadami.data.download.stream

import com.sf.tadami.storage.StorageFile
import com.sf.tadami.data.download.FileProgress
import com.sf.tadami.data.download.SegmentMetadata
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
 * parallel and written in order, so a resume only needs the count of written segments. Where each
 * segment ended up is recorded too, for the sidecar: the cast proxy serves the file as a playlist
 * again.
 */
class HlsDownload(private val http: MediaHttp) {

    /** The separate audio of the chosen variant, to download next to the video. */
    class AudioRendition(val url: String, val lang: String)

    /** Where a resumed download goes on: after [segments] segments, [bytes] long. */
    data class ResumePoint(val segments: Int, val bytes: Long)

    private class Segment(
        val url: String,
        val range: String?,
        val keyUrl: String?,
        val iv: ByteArray?,
        val durationUs: Long,
        val discontinuitySequence: Int,
    )

    /**
     * Downloads the playlist at [url] into [dir] as `<fileBase>.<ext>`. [playlist] is its content
     * when already read, [url] then being where it was read from. For a multivariant playlist, the
     * variant closest to [quality].
     *
     * @return the file, with its segments when their sizes are all known.
     */
    suspend fun download(
        url: String,
        playlist: String?,
        headers: Headers?,
        dir: StorageFile,
        fileBase: String,
        quality: String,
        audio: Boolean,
        resume: FileProgress?,
        onAudioRendition: (AudioRendition) -> Unit,
        onFileProgress: (FileProgress) -> Unit,
        onProgress: (Float) -> Unit,
    ): DownloadedFile {
        val (baseUrl, text) = if (playlist != null) url to playlist else fetchPlaylist(url, headers)
        var parsed = parse(baseUrl, text)
        if (parsed is M3u8Multivariant) {
            val variant = chooseVariant(parsed, quality) ?: throw StreamFailedException(StreamFailure.PLAYLIST, "No variant in the playlist")
            audioRenditionOf(parsed, variant)?.let(onAudioRendition)
            val (mediaUrl, mediaText) = fetchPlaylist(variant.url, headers)
            parsed = parse(mediaUrl, mediaText)
        }
        val media = parsed as? M3u8Media ?: throw StreamFailedException(StreamFailure.PLAYLIST, "No media playlist")
        checkSupported(media)
        if (!audio && MediaCheck.isTooShort(media.durationUs / 1000)) {
            throw StreamFailedException(StreamFailure.TOO_SHORT, "The video lasts ${media.durationUs / 1_000_000} s only")
        }

        val segments = media.segments
            .withIndex()
            // A gap has no data
            .filterNot { (_, segment) -> segment.hasGapTag }
            .map { (index, segment) -> segmentOf(segment, media.mediaSequence + index) }
        val init = media.segments.firstNotNullOfOrNull { it.initialization }?.let { initSegmentOf(it) }
        return writeSegments(segments, init, headers, dir, fileBase, audio, resume, onFileProgress, onProgress)
    }

    /** Where the playlist was read from, after the redirects, and its content */
    private suspend fun fetchPlaylist(url: String, headers: Headers?): Pair<String, String> {
        return http.read(url, headers) { response ->
            // Relative URIs are relative to where a redirect led
            response.request.url.toString() to response.body.string()
        }
    }

    private fun parse(url: String, text: String): M3u8Playlist {
        if (!MediaCheck.isPlaylist(text.take(MediaCheck.SNIFF_BYTES).encodeToByteArray())) {
            throw StreamFailedException(StreamFailure.PLAYLIST, "Not an HLS playlist")
        }
        // Kwik's segments keep their encoder number as IV after a gap
        val fixed = HlsSegmentIvs.fix(text) ?: text
        return try {
            M3u8.parse(url, fixed)
        } catch (e: IOException) {
            throw StreamFailedException(StreamFailure.PLAYLIST, "Unreadable playlist: ${e.message}", e)
        }
    }

    private fun chooseVariant(playlist: M3u8Multivariant, quality: String): M3u8Multivariant.Variant? {
        val variants = playlist.variants.mapIndexed { index, variant ->
            HlsVariants.Variant(
                index = index,
                height = variant.height,
                bandwidth = variant.bitrate,
                demuxedAudio = audioRenditions(playlist, variant).isNotEmpty(),
            )
        }
        return HlsVariants.chooseVariant(variants, quality)?.let { playlist.variants[it.index] }
    }

    // Renditions without a URI are the muxed audio, described
    private fun audioRenditions(
        playlist: M3u8Multivariant,
        variant: M3u8Multivariant.Variant,
    ): List<IndexedValue<M3u8Multivariant.Rendition>> {
        val group = variant.audioGroupId ?: return emptyList()
        return playlist.audios.withIndex().filter { (_, rendition) -> rendition.groupId == group && rendition.url != null }
    }

    private fun audioRenditionOf(
        playlist: M3u8Multivariant,
        variant: M3u8Multivariant.Variant,
    ): AudioRendition? {
        val renditions = audioRenditions(playlist, variant).map { (index, rendition) ->
            HlsVariants.AudioRendition(index, rendition.isDefault)
        }
        val chosen = playlist.audios[HlsVariants.chooseAudio(renditions)?.index ?: return null]
        return AudioRendition(chosen.url!!, chosen.language ?: chosen.name)
    }

    private fun checkSupported(media: M3u8Media) {
        if (!media.hasEndTag) throw StreamFailedException(StreamFailure.LIVE, "Live playlist")
        if (media.segments.isEmpty()) throw StreamFailedException(StreamFailure.PLAYLIST, "Empty playlist")
        if (media.hasDrm) {
            throw StreamFailedException(StreamFailure.DRM, "DRM protected playlist")
        }
        val inits = media.segments.mapTo(mutableSetOf()) { segment ->
            segment.initialization?.let { "${it.url}|${it.byteRange}" }
        }
        // A second init segment would need a second file, players don't read joined ones
        if (inits.size > 1) throw StreamFailedException(StreamFailure.UNSUPPORTED, "Several init segments")
    }

    private fun segmentOf(segment: M3u8Media.Segment, mediaSequence: Long): Segment {
        return Segment(
            url = segment.url,
            range = segment.byteRange?.let(::rangeHeader),
            keyUrl = segment.keyUri,
            iv = segment.keyUri?.let { HlsCrypto.iv(segment.encryptionIv, mediaSequence) },
            durationUs = segment.durationUs,
            discontinuitySequence = segment.discontinuitySequence,
        )
    }

    private fun initSegmentOf(init: M3u8Media.Initialization): Segment = Segment(
        url = init.url,
        range = init.byteRange?.let(::rangeHeader),
        keyUrl = null,
        iv = null,
        durationUs = 0L,
        discontinuitySequence = 0,
    )

    private fun rangeHeader(range: LongRange) = "bytes=${range.first}-${range.last}"

    private suspend fun writeSegments(
        segments: List<Segment>,
        init: Segment?,
        headers: Headers?,
        dir: StorageFile,
        fileBase: String,
        audio: Boolean,
        resume: FileProgress?,
        onFileProgress: (FileProgress) -> Unit,
        onProgress: (Float) -> Unit,
    ): DownloadedFile = coroutineScope {
        val count = segments.size
        val keys = Keys(headers)
        var file = resume?.takeIf { it.hls }?.let { dir.findFile(it.name) }
        val point = resumePoint(resume, count, file?.length())
        if (point == null) {
            file?.delete()
            file = null
        }
        var output: OutputStream? = file?.let { DownloadOutput.openAt(it, point!!.bytes) }
        var written = point?.bytes ?: 0L
        var done = point?.segments ?: 0
        // Unknown for good when resumed from a download that didn't record them
        val sizes = if (point == null) mutableListOf() else resumedSizes(resume, point)?.toMutableList()
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
                    output = DownloadOutput.openAt(target, 0)
                    onFileProgress(FileProgress(name = target.name.orEmpty(), hls = true, total = count.toLong(), sizes = sizes?.toList()))
                    if (init != null) {
                        val initData = fetch(init, headers, keys)
                        DownloadOutput.write(output, initData)
                        written += initData.size
                    }
                }
                DownloadOutput.write(output, data)
                written += data.size
                done++
                sizes?.add(data.size.toLong())
                onFileProgress(
                    FileProgress(
                        name = target.name.orEmpty(),
                        hls = true,
                        bytes = written,
                        segments = done,
                        total = count.toLong(),
                        sizes = sizes?.toList(),
                    ),
                )
                onProgress(done.toFloat() / count)
            }
            val joined = file ?: throw StreamFailedException(StreamFailure.TOO_SMALL, "Nothing downloaded")
            DownloadedFile(
                joined,
                segmentIndex(written, sizes, segments.map { it.durationUs }, segments.map { it.discontinuitySequence }),
            )
        } finally {
            output?.close()
        }
    }

    private suspend fun fetch(segment: Segment, headers: Headers?, keys: Keys): ByteArray {
        val data = http.bytes(segment.url, headers, segment.range)
        if (data.isEmpty()) return data
        if (segment.keyUrl == null) {
            if (MediaCheck.looksLikeText(data.copyOf(minOf(data.size, MediaCheck.SNIFF_BYTES)))) {
                throw StreamFailedException(StreamFailure.NOT_VIDEO, "A segment is a text or HTML page")
            }
            return data
        }
        return try {
            HlsCrypto.decrypt(data, keys.get(segment.keyUrl), segment.iv!!)
        } catch (e: GeneralSecurityException) {
            throw StreamFailedException(StreamFailure.DECRYPTION, "Can't decrypt a segment", e)
        }
    }

    /** The AES keys, fetched once per URI with the stream's headers. */
    private inner class Keys(private val headers: Headers?) {
        private val mutex = Mutex()
        private val keys = mutableMapOf<String, ByteArray>()

        suspend fun get(url: String): ByteArray = mutex.withLock {
            keys[url] ?: http.bytes(url, headers).also { key ->
                if (key.size != KEY_SIZE) throw StreamFailedException(StreamFailure.DECRYPTION, "AES-128 key of ${key.size} bytes")
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

        /** The sizes of the segments written before [point], null when they weren't all recorded. */
        fun resumedSizes(resume: FileProgress?, point: ResumePoint): List<Long>? {
            return resume?.sizes?.takeIf { it.size >= point.segments }?.take(point.segments)
        }

        /**
         * Where each segment is in the joined file of [fileLength] bytes: they follow each other
         * after the init segment, whatever is left before them. Null when a size is unknown.
         */
        fun segmentIndex(
            fileLength: Long,
            sizes: List<Long>?,
            durationsUs: List<Long>,
            discontinuitySequences: List<Int>,
        ): List<SegmentMetadata>? {
            if (sizes == null || sizes.size != durationsUs.size || sizes.size != discontinuitySequences.size) return null
            var offset = fileLength - sizes.sum()
            if (offset < 0) return null
            return sizes.mapIndexed { index, size ->
                SegmentMetadata(
                    offset = offset,
                    length = size,
                    durationUs = durationsUs[index],
                    discontinuity = index > 0 && discontinuitySequences[index] != discontinuitySequences[index - 1],
                ).also { offset += size }
            }
        }
    }
}
