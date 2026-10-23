package com.sf.tadami.data.download

import android.content.Context
import com.hippo.unifile.UniFile
import com.sf.tadami.data.download.stream.DownloadOutput
import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewSidecar
import com.sf.tadami.utils.logcat
import logcat.LogPriority
import java.io.OutputStream

/**
 * The seekbar preview frames of a downloaded episode ([PreviewSidecar]), `<episode>.preview.tdp` in its folder
 * [dir] next to its [video] (see [DownloadNames.previewsFileName]): read and written through the storage folder,
 * as the downloads' other files. Deleted with the episode's folder. Storage access: off the main thread.
 */
class EpisodePreviews(val dir: UniFile, val episodeDirName: String, val video: UniFile) {

    /** The frames, null when there are none yet or they don't describe [video] (another version, a cut file). */
    fun read(): PreviewSidecar? {
        return try {
            val file = dir.findFile(DownloadNames.previewsFileName(episodeDirName)) ?: return null
            val bytes = file.openInputStream().use { it.readBytes() }
            PreviewSidecar.decode(bytes, video.length()).also {
                if (it == null) logcat(LogPriority.WARN, tag = TAG) { "Ignoring the unusable ${file.name}" }
            }
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e, tag = TAG) { "Couldn't read the preview frames of $episodeDirName" }
            null
        }
    }

    /**
     * Writes [sidecar], replacing any: under [DownloadNames.previewsTmpFileName] first, renamed once complete, so a
     * reader never sees half a file. Nothing when the video is gone or changed (the episode deleted or downloaded
     * again meanwhile): its folder is never made again. False when it wasn't written.
     */
    fun write(context: Context, sidecar: PreviewSidecar): Boolean = write(sidecar) { DownloadOutput.openAt(context, it, 0) }

    /** [write], the file written opened by [open]. */
    internal fun write(sidecar: PreviewSidecar, open: (UniFile) -> OutputStream): Boolean {
        val name = DownloadNames.previewsFileName(episodeDirName)
        var tmp: UniFile? = null
        return try {
            if (!video.exists() || video.length() != sidecar.videoLength) {
                logcat(tag = TAG) { "Not writing the preview frames of $episodeDirName: its video is gone or changed" }
                return false
            }
            val bytes = PreviewSidecar.encode(sidecar)
            tmp = DownloadOutput.getOrCreateFile(dir, DownloadNames.previewsTmpFileName(episodeDirName))
            open(tmp).use { DownloadOutput.write(it, bytes) }
            // A storage folder renames to "<name> (1)" rather than replace
            dir.findFile(name)?.delete()
            if (!tmp.renameTo(name) || tmp.name != name) {
                // Never left under a name that could be taken for something else
                tmp.delete()
                logcat(LogPriority.WARN, tag = TAG) { "Couldn't rename the preview frames of $episodeDirName" }
                return false
            }
            logcat(tag = TAG) { "Wrote $name: ${sidecar.frameCount} frames, ${bytes.size / 1024} KiB" }
            true
        } catch (e: Exception) {
            // Never left half written
            tmp?.let { runCatching { it.delete() } }
            logcat(LogPriority.WARN, e, tag = TAG) { "Couldn't write the preview frames of $episodeDirName" }
            false
        }
    }

    private companion object {
        const val TAG = "EpisodePreviews"
    }
}
