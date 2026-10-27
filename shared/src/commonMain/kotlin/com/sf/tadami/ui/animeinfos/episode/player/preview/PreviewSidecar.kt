package com.sf.tadami.ui.animeinfos.episode.player.preview

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import kotlin.math.abs

/**
 * The seekbar preview frames of a downloaded episode, stored next to its video (`<episode>.preview.tdp`, see
 * DownloadNames.previewsFileName), so a scrub finds every step ready as soon as the episode opens, on the phone
 * and on the cast receiver. One [Entry] per preview step of the video, as the phone player makes them
 * ([phonePreviewBucketOf] over [durationMs]): its JPEG (about [PREVIEW_FRAME_WIDTH_PX] wide, the cast's), or
 * none when the step gave no frame in time.
 *
 * Binary, big-endian ([encode]):
 * - magic `TDSP`, format [VERSION];
 * - [videoLength] (long), [durationMs] (long), [stepMs] (long), [frameWidth] and [frameHeight] (ints);
 * - the entry count (int), then per entry: its step (int), the frame's time (long, ms, -1 without a frame),
 *   the JPEG's length (int, 0 without a frame) and the JPEG.
 *
 * [decode] refuses another version, another video (its length), a step size that isn't the one of its
 * duration, and a truncated or overlong file: such a sidecar is ignored, then made again.
 */
class PreviewSidecar(
    /** Bytes of the video file the frames come from */
    val videoLength: Long,
    /** The video's duration, as the player reads the file */
    val durationMs: Long,
    /** [previewBucketSizeMs] of [durationMs] */
    val stepMs: Long,
    val frameWidth: Int,
    val frameHeight: Int,
    val entries: List<Entry>,
) {
    /** The step [step]'s frame: its JPEG, taken at [timeMs] of the video; no JPEG when the step has none. */
    class Entry(val step: Int, val timeMs: Long, val jpeg: ByteArray?) {
        val hasFrame: Boolean
            get() = jpeg != null
    }

    private val byStep = entries.associateBy { it.step }

    /** Frames in the sidecar, the steps without one apart. */
    val frameCount: Int
        get() = entries.count { it.hasFrame }

    /**
     * Whether the frames suit a player of the same video whose duration is [durationMs]: within
     * [PREVIEW_SIDECAR_DURATION_TOLERANCE_MS], a frame then stays inside the player's step ([entryAt]).
     */
    fun matchesDuration(durationMs: Long): Boolean =
        durationMs > 0L && abs(durationMs - this.durationMs) <= PREVIEW_SIDECAR_DURATION_TOLERANCE_MS

    /**
     * The entry standing for the step [range] of a player's own timeline (its own duration, maybe a few ms off
     * this one's): the sidecar step of its middle; a leftover end step (see [phonePreviewBucketOf]) the one
     * before it. Null when the sidecar has no entry there.
     */
    fun entryAt(range: LongRange): Entry? {
        val middle = (range.first + range.last) / 2
        return byStep[previewBucketOf(middle, durationMs)] ?: byStep[phonePreviewBucketOf(middle, durationMs)]
    }

    companion object {
        const val VERSION = 1
        private const val MAGIC = 0x54445350 // "TDSP"

        /** A step past which an entry can't be (a 100 h video of 30 s steps): a corrupted count. */
        private const val MAX_ENTRIES = 12_000

        /** No JPEG of a preview frame is this big: a corrupted length. */
        private const val MAX_JPEG_BYTES = 1024 * 1024

        fun encode(sidecar: PreviewSidecar): ByteArray {
            val bytes = ByteArrayOutputStream(sidecar.entries.sumOf { (it.jpeg?.size ?: 0) + 16 } + 64)
            DataOutputStream(bytes).use { out ->
                out.writeInt(MAGIC)
                out.writeInt(VERSION)
                out.writeLong(sidecar.videoLength)
                out.writeLong(sidecar.durationMs)
                out.writeLong(sidecar.stepMs)
                out.writeInt(sidecar.frameWidth)
                out.writeInt(sidecar.frameHeight)
                out.writeInt(sidecar.entries.size)
                sidecar.entries.forEach { entry ->
                    val jpeg = entry.jpeg
                    out.writeInt(entry.step)
                    out.writeLong(if (jpeg != null) entry.timeMs else -1L)
                    out.writeInt(jpeg?.size ?: 0)
                    jpeg?.let(out::write)
                }
            }
            return bytes.toByteArray()
        }

        /** The sidecar in [bytes], if it describes a video of [videoLength] bytes; null when it isn't usable. */
        fun decode(bytes: ByteArray, videoLength: Long): PreviewSidecar? {
            return try {
                DataInputStream(bytes.inputStream()).use { input ->
                    if (input.readInt() != MAGIC || input.readInt() != VERSION) return null
                    val length = input.readLong()
                    val durationMs = input.readLong()
                    val stepMs = input.readLong()
                    val width = input.readInt()
                    val height = input.readInt()
                    if (length != videoLength || durationMs <= 0L || stepMs != previewBucketSizeMs(durationMs)) return null
                    if (width !in 1..PREVIEW_FRAME_WIDTH_PX || height !in 1..PREVIEW_FRAME_WIDTH_PX) return null
                    val count = input.readInt()
                    if (count !in 0..MAX_ENTRIES) return null
                    val lastStep = previewBucketOf(durationMs - 1, durationMs)
                    val entries = List(count) {
                        val step = input.readInt()
                        val timeMs = input.readLong()
                        val size = input.readInt()
                        if (step !in 0..lastStep || size !in 0..MAX_JPEG_BYTES) return null
                        val jpeg = if (size > 0) ByteArray(size).also(input::readFully) else null
                        Entry(step, timeMs, jpeg)
                    }
                    // Anything after the last entry: not a file this version wrote
                    if (input.read() != -1) return null
                    if (entries.distinctBy { it.step }.size != entries.size) return null
                    PreviewSidecar(length, durationMs, stepMs, width, height, entries)
                }
            } catch (_: EOFException) {
                // Truncated
                null
            } catch (_: IOException) {
                null
            }
        }
    }
}

/** How far a sidecar's duration may be from the player's: well under a step, so each frame stays in its step. */
const val PREVIEW_SIDECAR_DURATION_TOLERANCE_MS = 1_000L
