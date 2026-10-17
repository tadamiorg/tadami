package com.sf.tadami.data.download.stream

import android.content.Context
import com.hippo.unifile.UniFile
import com.sf.tadami.data.download.DownloadNames
import com.sf.tadami.data.download.FileProgress
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.utils.logcat
import kotlinx.coroutines.CancellationException
import logcat.LogPriority
import okhttp3.Headers
import okhttp3.OkHttpClient

/**
 * Downloads one [StreamSource] into an episode's tmp directory: the video, its separate audio
 * (the HLS rendition and the stream's audio tracks), and its subtitles. HLS or progressive is told
 * by the URL, else by the response.
 */
class StreamDownloader(private val context: Context, client: OkHttpClient) {

    private val http = MediaHttp(client)
    private val progressive = ProgressiveDownload(context, http)
    private val hls = HlsDownload(context, http)

    /**
     * @param resume the progress of each file of this stream, by role.
     * @param onFileProgress called with a role ("video", "audio0"...) and its file's progress, to
     * save for a resume.
     * @return the video file and the separate audio files.
     */
    suspend fun download(
        stream: StreamSource,
        dir: UniFile,
        episodeDirName: String,
        resume: Map<String, FileProgress>,
        onFileProgress: (String, FileProgress) -> Unit,
        onProgress: (Float) -> Unit,
    ): DownloadedStream {
        val headers = stream.headers
        val progress = Progress(onProgress)
        progress.add(VIDEO, VIDEO_WEIGHT)
        stream.audioTracks.indices.forEach { progress.add(audioRole(it), AUDIO_WEIGHT) }

        var rendition: HlsDownload.AudioRendition? = null
        val video = media(
            role = VIDEO,
            url = stream.url,
            headers = headers,
            dir = dir,
            fileBase = episodeDirName,
            quality = stream.quality,
            audio = false,
            resume = resume[VIDEO],
            onAudioRendition = {
                rendition = it
                progress.add(audioRole(stream.audioTracks.size), AUDIO_WEIGHT)
            },
            onFileProgress = onFileProgress,
            progress = progress,
        )
        if (MediaCheck.isTooSmall(video.length())) {
            throw StreamFailedException(StreamFailure.TOO_SMALL, "The video is ${video.length()} bytes only")
        }

        val audios = stream.audioTracks.map { it.url to it.lang } + listOfNotNull(rendition?.let { it.url to it.lang })
        val audioNames = DownloadNames.trackNames(audios.map { it.second })
        val audioFiles = audios.mapIndexed { index, (url, _) ->
            media(
                role = audioRole(index),
                url = url,
                headers = headers,
                dir = dir,
                fileBase = DownloadNames.audioFileBase(episodeDirName, audioNames[index]),
                quality = "",
                audio = true,
                resume = resume[audioRole(index)],
                onAudioRendition = {},
                onFileProgress = onFileProgress,
                progress = progress,
            )
        }

        downloadSubtitles(stream, headers, dir, episodeDirName)
        return DownloadedStream(DownloadedFile(video), audioFiles.map(::DownloadedFile))
    }

    private suspend fun media(
        role: String,
        url: String,
        headers: Headers?,
        dir: UniFile,
        fileBase: String,
        quality: String,
        audio: Boolean,
        resume: FileProgress?,
        onAudioRendition: (HlsDownload.AudioRendition) -> Unit,
        onFileProgress: (String, FileProgress) -> Unit,
        progress: Progress,
    ): UniFile {
        val saveProgress = { file: FileProgress -> onFileProgress(role, file) }
        val reportProgress = { fraction: Float -> progress.update(role, fraction) }
        suspend fun hlsDownload(playlistUrl: String, playlist: String?) = hls.download(
            playlistUrl, playlist, headers, dir, fileBase, quality, audio, resume,
            onAudioRendition, saveProgress, reportProgress,
        )
        if (resume != null) {
            return if (resume.hls) {
                hlsDownload(url, null)
            } else {
                progressive.download(url, headers, dir, fileBase, resume, null, saveProgress, reportProgress)
            }
        }
        if (MediaCheck.isPlaylistUrl(url)) return hlsDownload(url, null)

        // The response tells: a playlist is read here, a video goes on from this very response
        val response = http.open(url, headers)
        val isPlaylist = try {
            MediaCheck.isPlaylistContentType(response.header("Content-Type")) ||
                MediaCheck.isPlaylist(response.peekBody(MediaCheck.SNIFF_BYTES.toLong()).bytes())
        } catch (e: Exception) {
            response.close()
            throw e
        }
        if (isPlaylist) {
            val (finalUrl, playlist) = response.use {
                http.readCancellable(it) { read -> read.request.url.toString() to read.body.string() }
            }
            return hlsDownload(finalUrl, playlist)
        }
        return progressive.download(url, headers, dir, fileBase, null, response, saveProgress, reportProgress)
    }

    /** Best effort: an episode without its subtitles is still worth having. */
    private suspend fun downloadSubtitles(stream: StreamSource, headers: Headers?, dir: UniFile, episodeDirName: String) {
        val tracks = stream.subtitleTracks
        val names = DownloadNames.trackNames(tracks.map { it.lang })
        tracks.forEachIndexed { index, track ->
            try {
                val data = http.bytes(track.url, headers)
                val head = data.copyOf(minOf(data.size, SUBTITLE_SNIFF_BYTES))
                val start = head.decodeToString().trimStart('﻿', ' ', '\r', '\n').lowercase()
                if (data.isEmpty() || start.startsWith("<!doctype html") || start.startsWith("<html")) {
                    logcat(LogPriority.WARN) { "Skipped the ${track.lang} subtitles: not a subtitle file" }
                    return@forEachIndexed
                }
                val extension = MediaCheck.subtitleExtension(track.mimeType, track.url, head)
                val name = DownloadNames.subtitleFileName(episodeDirName, names[index], extension)
                DownloadOutput.storage { dir.findFile(name)?.delete() }
                val file = DownloadOutput.getOrCreateFile(dir, name)
                DownloadOutput.openAt(context, file, 0).use { DownloadOutput.write(it, data) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: StorageException) {
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Couldn't download the ${track.lang} subtitles" }
            }
        }
    }

    /** The files of a downloaded stream, in the tmp dir. */
    class DownloadedStream(val video: DownloadedFile, val audios: List<DownloadedFile>)

    class DownloadedFile(val file: UniFile)

    /** The progress of the whole stream, each file weighted. */
    private class Progress(private val onProgress: (Float) -> Unit) {
        private val weights = mutableMapOf<String, Float>()
        private val fractions = mutableMapOf<String, Float>()

        @Synchronized
        fun add(role: String, weight: Float) {
            weights[role] = weight
        }

        @Synchronized
        fun update(role: String, fraction: Float) {
            fractions[role] = fraction
            val total = weights.values.sum()
            val done = weights.entries.sumOf { (key, weight) -> ((fractions[key] ?: 0f) * weight).toDouble() }
            onProgress((done / total).toFloat())
        }
    }

    companion object {
        const val VIDEO = "video"
        private const val VIDEO_WEIGHT = 4f
        private const val AUDIO_WEIGHT = 1f
        private const val SUBTITLE_SNIFF_BYTES = 512

        fun audioRole(index: Int) = "audio$index"
    }
}
