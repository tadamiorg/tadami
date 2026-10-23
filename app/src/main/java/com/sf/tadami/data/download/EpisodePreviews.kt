package com.sf.tadami.data.download

import android.content.Context
import com.hippo.unifile.UniFile
import com.sf.tadami.data.download.stream.DownloadOutput
import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewSidecar
import com.sf.tadami.utils.logcat
import logcat.LogPriority
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * The seekbar preview frames of a downloaded episode ([PreviewSidecar]), `<episode>.preview.tdp` in its folder
 * [dir] next to its [video] (see [DownloadNames.previewsFileName]): read and written through the storage folder,
 * as the downloads' other files. Deleted with the episode's folder. Storage access: off the main thread.
 */
class EpisodePreviews(val dir: UniFile, val episodeDirName: String, val video: UniFile) {

    /** The frames, null when there are none yet or they don't describe [video] (another version, a cut file). */
    fun read(): PreviewSidecar? = synchronized(lock()) { readFile()?.second }

    /** The file of the frames as it is, for another device to decode: null when [read] would give null. */
    fun readBytes(): ByteArray? = synchronized(lock()) { readFile()?.first }

    private fun readFile(): Pair<ByteArray, PreviewSidecar>? {
        return try {
            val file = dir.findFile(DownloadNames.previewsFileName(episodeDirName)) ?: return null
            val bytes = file.openInputStream().use { it.readBytes() }
            val sidecar = PreviewSidecar.decode(bytes, video.length())
            if (sidecar == null) {
                logcat(LogPriority.WARN, tag = TAG) { "Ignoring the unusable ${file.name}" }
                return null
            }
            bytes to sidecar
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e, tag = TAG) { "Couldn't read the preview frames of $episodeDirName" }
            null
        }
    }

    /**
     * Writes [sidecar], replacing any: under [DownloadNames.previewsTmpFileName] first, renamed once complete, so a
     * reader never sees half a file. Nothing when the video is gone or changed (the episode deleted or downloaded
     * again meanwhile): its folder is never made again. Nothing either when valid frames that [keeps] (any, by
     * default) are there already: made meanwhile by another writer (the frames made after the download, the
     * player's prefetch, a cast). False when it wasn't written.
     *
     * One reader or writer of an episode's frames at a time in the whole app: the writers share the temporary name,
     * and a reader never finds the frames missing between the old ones deleted and the new ones renamed.
     */
    fun write(context: Context, sidecar: PreviewSidecar, keeps: (PreviewSidecar) -> Boolean = { true }): Boolean =
        write(sidecar, keeps) { DownloadOutput.openAt(context, it, 0) }

    /** [write], the file written opened by [open]. */
    internal fun write(sidecar: PreviewSidecar, keeps: (PreviewSidecar) -> Boolean = { true }, open: (UniFile) -> OutputStream): Boolean =
        synchronized(lock()) { writeFile(sidecar, keeps, open) }

    private fun writeFile(sidecar: PreviewSidecar, keeps: (PreviewSidecar) -> Boolean, open: (UniFile) -> OutputStream): Boolean {
        val name = DownloadNames.previewsFileName(episodeDirName)
        var tmp: UniFile? = null
        return try {
            if (!video.exists() || video.length() != sidecar.videoLength) {
                logcat(tag = TAG) { "Not writing the preview frames of $episodeDirName: its video is gone or changed" }
                return false
            }
            if (readFile()?.second?.let(keeps) == true) {
                logcat(tag = TAG) { "Not writing the preview frames of $episodeDirName: made meanwhile" }
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

    private fun lock(): Any = LOCKS.getOrPut(episodeDirName) { Any() }

    private companion object {
        const val TAG = "EpisodePreviews"

        /** By episode folder name: two animes' folders named alike share theirs, which only makes them wait. */
        val LOCKS = ConcurrentHashMap<String, Any>()
    }
}
