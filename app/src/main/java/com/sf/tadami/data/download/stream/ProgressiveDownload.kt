package com.sf.tadami.data.download.stream

import android.content.Context
import com.hippo.unifile.UniFile
import com.sf.tadami.data.download.FileProgress
import com.sf.tadami.network.HttpException
import com.sf.tadami.utils.isOnline
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.Headers
import okhttp3.Response
import java.io.IOException

/** One progressive file (mp4, mkv...), resumed with Range requests where it stopped. */
class ProgressiveDownload(private val context: Context, private val http: MediaHttp) {

    /**
     * Downloads [url] into [dir] as `<fileBase>.<ext>`, the extension sniffed from the content.
     * [resume] is where a previous attempt stopped, [opened] a response already received for
     * [url] from its start.
     */
    suspend fun download(
        url: String,
        headers: Headers?,
        dir: UniFile,
        fileBase: String,
        resume: FileProgress?,
        opened: Response?,
        onFileProgress: (FileProgress) -> Unit,
        onProgress: (Float) -> Unit,
    ): UniFile {
        var file = resume?.takeIf { !it.hls }?.let { dir.findFile(it.name) }
        var expectedTotal = resume?.total ?: -1L
        var pending = opened
        var failures = 0
        while (true) {
            currentCoroutineContext().ensureActive()
            val start = if (pending != null) 0L else file?.length()?.coerceAtLeast(0L) ?: 0L
            if (start > 0 && start == expectedTotal) return file!!
            try {
                val response = pending ?: http.open(url, headers, if (start > 0) "bytes=$start-" else null)
                pending = null
                val target = file
                val result = http.readCancellable(response) {
                    it.use { write(it, url, start, expectedTotal, target, dir, fileBase, onFileProgress, onProgress) }
                }
                file = result.file
                if (result.startedOver) expectedTotal = result.total
                val length = result.file.length()
                if (result.total <= 0 || length >= result.total) return result.file
                // Ended early without an error: go on from there
                if (++failures > MAX_RESUMES) {
                    throw StreamFailedException("The video keeps ending at $length of ${result.total} bytes")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: StartOverException) {
                // What was saved doesn't continue what is served now
                file?.delete()
                file = null
                expectedTotal = -1
            } catch (e: HttpException) {
                if (e.code != 416 || start == 0L) throw e
                // The saved part is no longer served
                file?.delete()
                file = null
                expectedTotal = -1
            } catch (e: IOException) {
                if (e is NetworkLostException) throw e
                if (!context.isOnline()) throw NetworkLostException(e)
                if (++failures > MAX_RESUMES) throw e
            }
        }
    }

    private class StartOverException : IOException()

    private class WriteResult(val file: UniFile, val total: Long, val startedOver: Boolean)

    private fun write(
        response: Response,
        url: String,
        start: Long,
        expectedTotal: Long,
        existing: UniFile?,
        dir: UniFile,
        fileBase: String,
        onFileProgress: (FileProgress) -> Unit,
        onProgress: (Float) -> Unit,
    ): WriteResult {
        val partial = start > 0 && response.code == 206
        val contentLength = response.body.contentLength()
        val total = if (partial) {
            contentRangeTotal(response.header("Content-Range"))
                ?: contentLength.takeIf { it >= 0 }?.plus(start)
                ?: -1L
        } else {
            contentLength
        }
        // Another file behind the same stream
        if (partial && expectedTotal > 0 && total > 0 && total != expectedTotal) throw StartOverException()

        var file = existing
        // The server ignored the Range: the whole file comes again
        val position = if (partial) start else 0L
        if (position == 0L) {
            val contentType = response.header("Content-Type")
            if (MediaCheck.isTextContentType(contentType) || MediaCheck.isPlaylistContentType(contentType)) {
                throw StreamFailedException("Not a video: $contentType")
            }
            val head = response.peekBody(MediaCheck.SNIFF_BYTES.toLong()).bytes()
            if (MediaCheck.looksLikeText(head)) throw StreamFailedException("Not a video: a text or HTML page")
            if (file == null) {
                val extension = MediaCheck.videoExtension(contentType, url, head)
                file = DownloadOutput.getOrCreateFile(dir, "$fileBase.$extension")
            }
            // Before any byte, so a resume finds the file
            onFileProgress(FileProgress(name = file.name.orEmpty(), hls = false, total = total))
        }
        val target = file ?: throw StartOverException()
        val fileProgress = { bytes: Long ->
            onFileProgress(FileProgress(name = target.name.orEmpty(), hls = false, bytes = bytes, total = total))
        }
        if (position > 0) fileProgress(position)

        DownloadOutput.openAt(context, target, position).use { output ->
            val source = response.body.source()
            val buffer = ByteArray(BUFFER_SIZE)
            var written = position
            var reported = -1
            var savedBytes = position
            while (true) {
                val read = source.read(buffer)
                if (read == -1) break
                DownloadOutput.write(output, buffer, 0, read)
                written += read
                // For the progress shown; a resume reads the file length
                if (written - savedBytes >= PROGRESS_BYTES) {
                    savedBytes = written
                    fileProgress(written)
                }
                if (total > 0) {
                    val permille = (written * 1000 / total).toInt()
                    if (permille != reported) {
                        reported = permille
                        onProgress(permille / 1000f)
                    }
                }
            }
        }
        return WriteResult(target, total, startedOver = position == 0L)
    }

    companion object {
        private const val BUFFER_SIZE = 64 * 1024
        private const val MAX_RESUMES = 5
        private const val PROGRESS_BYTES = 2L * 1024 * 1024

        /** The full length in "bytes 100-999/1000", null when unknown ("*"). */
        fun contentRangeTotal(header: String?): Long? {
            return header?.substringAfterLast('/', "")?.trim()?.toLongOrNull()
        }
    }
}
