package com.sf.tadami.data.download

import com.sf.tadami.utils.logcat
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import logcat.LogPriority
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * The download queue, kept in the app's private files so it survives a process death. Not in the
 * DataStore nor in the database: the downloads stay out of the backups.
 */
class DownloadStore(private val file: File, private val json: Json = DEFAULT_JSON) {

    @Serializable
    data class Entry(
        val episodeId: Long,
        val animeId: Long,
        val sourceId: Long,
        val dirName: String,
    )

    /** The entries in queue order, empty when there is none or the file can't be read. */
    fun restore(): List<Entry> {
        return try {
            if (!file.exists()) return emptyList()
            json.decodeFromString<List<Entry>>(file.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(entries: List<Entry>) {
        if (entries.isEmpty()) {
            file.delete()
            return
        }
        StoreFiles.writeAtomically(file, json.encodeToString(entries))
    }

    companion object {
        internal val DEFAULT_JSON = Json { ignoreUnknownKeys = true }
    }
}

/**
 * The resume data of the downloads in progress, one file per episode, see [ResumeData]. Written
 * after each HLS segment: private files are cheap, unlike the storage folder.
 */
class ResumeStore(private val dir: File, private val json: Json = DownloadStore.DEFAULT_JSON) {

    fun load(episodeId: Long): ResumeData? {
        return try {
            val file = fileOf(episodeId)
            if (!file.exists()) return null
            json.decodeFromString<ResumeData>(file.readText())
        } catch (_: Exception) {
            null
        }
    }

    fun save(episodeId: Long, data: ResumeData) {
        StoreFiles.writeAtomically(fileOf(episodeId), json.encodeToString(data))
    }

    fun delete(episodeId: Long) {
        fileOf(episodeId).delete()
    }

    private fun fileOf(episodeId: Long) = File(dir, "$episodeId.json")
}

/**
 * Where a download stopped. [stream] is the key of the stream in progress (its URL may be signed
 * again on the next fetch), [failed] the keys of the streams that failed, [files] the progress of
 * each file of the stream in progress, by role ("video", "audio0", ...).
 */
@Serializable
data class ResumeData(
    val stream: String? = null,
    val failed: List<String> = emptyList(),
    val files: Map<String, FileProgress> = emptyMap(),
)

/**
 * Progress of one file. Progressive: [bytes] written so far, every 2 MiB, only shown (a resume
 * reads the file length), [total] is the full length. HLS: [segments] segments were written in
 * order, [bytes] long, out of [total] segments.
 */
@Serializable
data class FileProgress(
    val name: String,
    val hls: Boolean,
    val bytes: Long = 0,
    val segments: Int = 0,
    val total: Long = -1,
)

private object StoreFiles {
    // A failed write keeps the previous content: losing a resume point or a queue change isn't worth a crash
    fun writeAtomically(file: File, text: String) {
        val tmp = File(file.parentFile, "${file.name}.tmp")
        try {
            file.parentFile?.mkdirs()
            tmp.writeText(text)
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: IOException) {
            tmp.delete()
            logcat(LogPriority.WARN, e) { "Couldn't write ${file.name}" }
        }
    }
}
