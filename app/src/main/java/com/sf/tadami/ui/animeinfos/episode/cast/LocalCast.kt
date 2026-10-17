package com.sf.tadami.ui.animeinfos.episode.cast

import android.content.Context
import android.util.Log
import com.hippo.unifile.UniFile
import com.sf.tadami.R
import com.sf.tadami.data.download.EpisodeMetadata
import com.sf.tadami.data.download.SegmentMetadata
import com.sf.tadami.data.download.TadamiDownloadManager
import com.sf.tadami.data.download.stream.MediaCheck
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.ui.animeinfos.episode.cast.proxy.CastProxyServer
import com.sf.tadami.ui.animeinfos.episode.cast.proxy.LocalFile
import com.sf.tadami.ui.animeinfos.episode.cast.proxy.LocalMedia
import com.sf.tadami.ui.animeinfos.episode.cast.proxy.LocalPlaylist
import com.sf.tadami.ui.animeinfos.episode.cast.proxy.LocalPlaylists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * A downloaded episode is cast from the phone: the cast proxy serves its files to the receiver
 * ([CastProxyServer.serveLocal]). Its source's streams are cast instead when the receiver couldn't
 * play them: no sidecar (downloads made before it), no segment index, a container the receiver
 * doesn't play, files missing, or no proxy (not started, no LAN address).
 *
 * What the receiver gets is a stream like an online one, so it plays it unchanged:
 * - a progressive MP4 or WebM: the file, with Range;
 * - a joined MPEG-TS: an HLS media playlist of `EXT-X-BYTERANGE` segments over the file, from the
 *   sidecar's segment index;
 * - separate audio: each file its own byte-range playlist, an audio track of the stream (the web
 *   receiver merges them in its own master playlist, as for an online stream). The phone's master
 *   playlist, the audio as `EXT-X-MEDIA` renditions, is the load's content URL;
 * - subtitles: the files, read by the receiver as the online ones.
 */
object LocalCast {

    /** One file of the downloaded episode: its [name] in the episode folder, its content [uri], [length] bytes. */
    data class EpisodeFile(val name: String, val uri: String, val length: Long)

    sealed interface Plan {
        /**
         * Served by the proxy: [media], the names the cast stream's URLs end with ([audios] and
         * [subtitles] in the order given) and the [master] playlist, when separate audio has one.
         */
        data class Phone(
            val media: LocalMedia,
            val video: String,
            val audios: List<String>,
            val subtitles: List<String>,
            val master: String?,
        ) : Plan

        /** Cast from the source's streams, for [reason] (the logs) */
        data class Online(val reason: String) : Plan
    }

    /** The stream to cast, and the URL of the whole media (the master playlist, if any). */
    data class Media(val source: StreamSource, val contentUrl: String)

    private const val TAG = "LocalCast"
    private const val TS = "ts"
    private const val TS_MIME_TYPE = "video/mp2t"

    /** Progressive containers the receivers play from the file, to their name and MIME type */
    private val PROGRESSIVE = mapOf(
        "mp4" to ("video.mp4" to "video/mp4"),
        "m4v" to ("video.mp4" to "video/mp4"),
        "webm" to ("video.webm" to "video/webm"),
    )

    /**
     * How the downloaded episode is cast: [video], the separate [audios] with their language and the
     * [subtitles] with their MIME type, described by the sidecar [metadata].
     */
    fun plan(
        metadata: EpisodeMetadata?,
        video: EpisodeFile,
        audios: List<Pair<EpisodeFile, String>>,
        subtitles: List<Pair<EpisodeFile, String>>,
    ): Plan {
        if (metadata == null) return Plan.Online("no sidecar")
        if (metadata.video.name != video.name) return Plan.Online("the sidecar describes another video")
        // Each one described, none missing
        val audioMetadata = audios.map { (file, _) ->
            metadata.audios.firstOrNull { it.name == file.name } ?: return Plan.Online("${file.name} isn't in the sidecar")
        }
        if (audioMetadata.size != metadata.audios.size) return Plan.Online("an audio file is missing")

        val files = mutableMapOf<String, LocalFile>()
        val playlists = mutableMapOf<String, LocalPlaylist>()
        val container = metadata.video.container
        val castVideo = if (container == TS) {
            val segments = indexOf(metadata.video.segments, video) ?: return Plan.Online("no segment index")
            files["video.ts"] = LocalFile(video.uri, TS_MIME_TYPE)
            playlists["video.m3u8"] = LocalPlaylist.Media("video.ts", segments)
            "video.m3u8"
        } else {
            val (name, mimeType) = PROGRESSIVE[container] ?: return Plan.Online("a $container file isn't castable")
            // The receivers merge only playlists
            if (audios.isNotEmpty()) return Plan.Online("separate audio for a $container file")
            files[name] = LocalFile(video.uri, mimeType)
            name
        }

        val castAudios = audios.mapIndexed { index, (file, _) ->
            val audio = audioMetadata[index]
            if (audio.container != TS) return Plan.Online("a separate ${audio.container} audio isn't castable")
            val segments = indexOf(audio.segments, file) ?: return Plan.Online("no segment index for ${file.name}")
            files["audio$index.ts"] = LocalFile(file.uri, TS_MIME_TYPE)
            playlists["audio$index.m3u8"] = LocalPlaylist.Media("audio$index.ts", segments)
            "audio$index.m3u8"
        }
        val master = if (castAudios.isEmpty()) {
            null
        } else {
            val bandwidth = LocalPlaylists.peakBitrate(metadata.video.segments.orEmpty()) +
                audioMetadata.maxOf { LocalPlaylists.peakBitrate(it.segments.orEmpty()) }
            playlists["master.m3u8"] = LocalPlaylist.Master(
                video = castVideo,
                audios = castAudios.zip(audios) { name, (_, lang) -> name to lang },
                bandwidth = bandwidth,
            )
            "master.m3u8"
        }

        val castSubtitles = subtitles.mapIndexed { index, (file, mimeType) ->
            val extension = file.name.substringAfterLast('.', "").lowercase()
            val name = if (extension.isEmpty()) "sub$index" else "sub$index.$extension"
            files[name] = LocalFile(file.uri, mimeType)
            name
        }

        return Plan.Phone(
            media = LocalMedia(key = video.uri, files = files, playlists = playlists),
            video = castVideo,
            audios = castAudios,
            subtitles = castSubtitles,
            master = master,
        )
    }

    /**
     * [local], the downloaded stream as the phone plays it (its tracks in the order given to [plan]),
     * as the receiver gets it: the URLs of the proxy, from [urlPrefix], no headers.
     */
    fun castSource(local: StreamSource, plan: Plan.Phone, urlPrefix: String): StreamSource {
        return local.copy(
            url = urlPrefix + plan.video,
            headers = null,
            audioTracks = local.audioTracks.mapIndexed { index, track -> track.copy(url = urlPrefix + plan.audios[index]) },
            subtitleTracks = local.subtitleTracks.mapIndexed { index, track -> track.copy(url = urlPrefix + plan.subtitles[index]) },
        )
    }

    /**
     * The downloaded [episode] served by [proxy], to cast; null to cast its source's streams (the
     * reason logged).
     */
    suspend fun resolve(context: Context, episode: Episode, proxy: CastProxyServer?): Media? = withContext(Dispatchers.IO) {
        val baseUrl = proxy?.baseUrl()
        if (baseUrl == null) {
            Log.i(TAG, "Casting ${episode.name} online: the proxy isn't running or the phone has no LAN address")
            return@withContext null
        }
        val local = Injekt.get<TadamiDownloadManager>().getLocalEpisode(episode)
        if (local == null) {
            Log.i(TAG, "Casting ${episode.name} online: no downloaded files")
            return@withContext null
        }
        val plan = plan(
            metadata = local.metadata,
            video = local.video.toEpisodeFile(),
            audios = local.audios.map { (file, lang) -> file.toEpisodeFile() to lang },
            subtitles = local.subtitles.map { (file, track) -> file.toEpisodeFile() to MediaCheck.subtitleMimeType(track.extension) },
        )
        when (plan) {
            is Plan.Online -> {
                Log.i(TAG, "Casting ${episode.name} online: ${plan.reason}")
                null
            }
            is Plan.Phone -> {
                val urlPrefix = baseUrl + proxy.serveLocal(plan.media)
                Log.i(TAG, "Casting ${episode.name} from the phone: $urlPrefix${plan.master ?: plan.video}")
                Media(
                    source = castSource(local.toStreamSource(context.getString(R.string.episode_downloaded)), plan, urlPrefix),
                    contentUrl = urlPrefix + (plan.master ?: plan.video),
                )
            }
        }
    }

    // The whole file, segment after segment: the sidecar describes this very file
    private fun indexOf(segments: List<SegmentMetadata>?, file: EpisodeFile): List<SegmentMetadata>? {
        val last = segments?.lastOrNull() ?: return null
        return segments.takeIf { last.offset + last.length == file.length }
    }

    private fun UniFile.toEpisodeFile() = EpisodeFile(name.orEmpty(), uri.toString(), length())
}
