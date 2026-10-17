package com.sf.tadami.data.download

import com.hippo.unifile.UniFile
import com.sf.tadami.data.download.stream.MediaCheck
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.source.model.Track

/** The files of a downloaded episode, as the player plays them. */
class LocalEpisode(
    val video: UniFile,
    val audios: List<Pair<UniFile, String>>,
    val subtitles: List<Pair<UniFile, DownloadNames.TrackFile>>,
) {

    /** A stream of content URIs: the player reads them without network, the separate audio merged. */
    fun toStreamSource(name: String): StreamSource {
        return StreamSource(
            url = video.uri.toString(),
            fullName = name,
            subtitleTracks = subtitles.map { (file, track) ->
                Track.SubtitleTrack(
                    url = file.uri.toString(),
                    lang = track.lang,
                    mimeType = MediaCheck.subtitleMimeType(track.extension),
                )
            },
            audioTracks = audios.map { (file, lang) -> Track.AudioTrack(url = file.uri.toString(), lang = lang) },
        )
    }

    companion object {
        /** The files of the episode directory [dir], null without a video. */
        fun from(dir: UniFile, episodeDirName: String): LocalEpisode? {
            val files = dir.listFiles().orEmpty().filter { it.isFile }.associateBy { it.name.orEmpty() }
            val parsed = DownloadNames.parseFiles(episodeDirName, files.keys.toList())
            val video = parsed.video?.let(files::get) ?: return null
            return LocalEpisode(
                video = video,
                audios = parsed.audios.mapNotNull { track -> files[track.name]?.let { it to track.lang } },
                subtitles = parsed.subtitles.mapNotNull { track -> files[track.name]?.let { it to track } },
            )
        }
    }
}

/** A downloaded file, not a URL to fetch: casting needs the online streams instead. */
val StreamSource.isLocalFile: Boolean
    get() = url.startsWith("content://") || url.startsWith("file://") || url.startsWith("/")
