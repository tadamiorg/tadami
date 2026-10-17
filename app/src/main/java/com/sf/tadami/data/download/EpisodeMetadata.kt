package com.sf.tadami.data.download

import android.content.Context
import com.hippo.unifile.UniFile
import com.sf.tadami.data.download.stream.DownloadOutput
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.utils.logcat
import kotlinx.serialization.Serializable
import logcat.LogPriority

/**
 * The sidecar of a downloaded episode, `<episode>.json` next to its files (see
 * [DownloadNames.metadataFileName]): the stream it was downloaded from, and its media files.
 * Written in the tmp dir before the rename, so it is there with the episode or not at all.
 * Downloads made before it have none.
 */
@Serializable
data class EpisodeMetadata(
    /** No default: a default isn't written, a newer format must say it is */
    val version: Int,
    /** [StreamSource.fullName], e.g. "Voe - 720p" */
    val fullName: String,
    val server: String = "",
    val quality: String = "",
    val video: MediaFileMetadata,
    /** The separate audio files, in download order */
    val audios: List<MediaFileMetadata> = emptyList(),
) {
    companion object {
        const val VERSION = 1

        fun of(stream: StreamSource, video: UniFile, audios: List<UniFile>) = EpisodeMetadata(
            version = VERSION,
            fullName = stream.fullName,
            server = stream.server,
            quality = stream.quality,
            video = MediaFileMetadata.of(video),
            audios = audios.map(MediaFileMetadata::of),
        )

        fun encode(metadata: EpisodeMetadata): String = DownloadStore.DEFAULT_JSON.encodeToString(metadata)

        /** Null when unreadable, or written by a newer version whose format isn't known here. */
        fun decode(text: String): EpisodeMetadata? {
            return try {
                DownloadStore.DEFAULT_JSON.decodeFromString<EpisodeMetadata>(text).takeIf { it.version <= VERSION }
            } catch (_: Exception) {
                null
            }
        }

        /** The sidecar [file] of a downloaded episode, null when it can't be read. */
        fun read(file: UniFile): EpisodeMetadata? {
            return try {
                file.openInputStream().use { decode(it.readBytes().decodeToString()) }
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Couldn't read ${file.name}" }
                null
            }
        }

        /** Writes [metadata] as the sidecar of [episodeDirName] in [dir], replacing any. */
        fun write(context: Context, dir: UniFile, episodeDirName: String, metadata: EpisodeMetadata) {
            val name = DownloadNames.metadataFileName(episodeDirName)
            DownloadOutput.storage { dir.findFile(name)?.delete() }
            val file = DownloadOutput.getOrCreateFile(dir, name)
            DownloadOutput.openAt(context, file, 0).use { DownloadOutput.write(it, encode(metadata).encodeToByteArray()) }
        }
    }
}

/** One media file of a downloaded episode. */
@Serializable
data class MediaFileMetadata(
    val name: String,
    /** The container, as the file extension: ts, mp4, mkv, m4a, aac... */
    val container: String,
) {
    companion object {
        fun of(file: UniFile): MediaFileMetadata {
            val name = file.name.orEmpty()
            return MediaFileMetadata(name, name.substringAfterLast('.', "").lowercase())
        }
    }
}
