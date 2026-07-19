package com.sf.tadami.ui.animeinfos.episode.player.subtitles.webvtt

import androidx.media3.common.C
import androidx.media3.common.text.Cue
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.extractor.text.Subtitle
import androidx.media3.extractor.text.webvtt.WebvttCueInfo
import java.util.Arrays
import java.util.Collections

@UnstableApi
internal class WebvttSubtitle(cueInfos: List<WebvttCueInfo>) : Subtitle {
    private val cueInfos: List<WebvttCueInfo> = Collections.unmodifiableList(ArrayList(cueInfos))
    private val cueTimesUs = LongArray(2 * cueInfos.size)
    private val sortedCueTimesUs: LongArray

    init {
        for (cueIndex in cueInfos.indices) {
            val cueInfo = cueInfos[cueIndex]
            val arrayIndex = cueIndex * 2
            cueTimesUs[arrayIndex] = cueInfo.startTimeUs
            cueTimesUs[arrayIndex + 1] = cueInfo.endTimeUs
        }
        sortedCueTimesUs = cueTimesUs.copyOf(cueTimesUs.size)
        Arrays.sort(sortedCueTimesUs)
    }

    override fun getNextEventTimeIndex(timeUs: Long): Int {
        val index = Util.binarySearchCeil(sortedCueTimesUs, timeUs, false, false)
        return if (index < sortedCueTimesUs.size) index else C.INDEX_UNSET
    }

    override fun getEventTimeCount(): Int {
        return sortedCueTimesUs.size
    }

    override fun getEventTime(index: Int): Long {
        Assertions.checkArgument(index >= 0)
        Assertions.checkArgument(index < sortedCueTimesUs.size)
        return sortedCueTimesUs[index]
    }

    override fun getCues(timeUs: Long): List<Cue> {
        val activeCuesWithTiming = mutableListOf<ActiveCue>()

        // First pass: Collect all active cues at current time
        for (i in cueInfos.indices) {
            if (cueTimesUs[i * 2] <= timeUs && timeUs < cueTimesUs[i * 2 + 1]) {
                val cueInfo = cueInfos[i]
                val activeCue = ActiveCue(
                    cue = cueInfo.cue,
                    startTimeUs = cueInfo.startTimeUs,
                    endTimeUs = cueInfo.endTimeUs,
                    lineCount = countTextLines(cueInfo.cue.text?.toString() ?: "")
                )
                activeCuesWithTiming.add(activeCue)
            }
        }

        // Sort cues by start time, then by end time for equal starts
        activeCuesWithTiming.sortWith(compareBy<ActiveCue>
        { it.startTimeUs }
            .thenBy { it.endTimeUs }
        )

        return positionCuesWithoutOverlap(activeCuesWithTiming)
    }

    private data class ActiveCue(
        val cue: Cue,
        val startTimeUs: Long,
        val endTimeUs: Long,
        val lineCount: Int
    )

    private fun countTextLines(text: String): Int {
        // Count the number of actual lines in the text
        return text.split("\n").size
    }

    /**
     * Sticky line assignment, keyed by cue start time. Once a cue is placed it keeps its line for its whole
     * lifetime, so a neighbour appearing or ending never shifts it (previously every interval recomputed all
     * lines from the active set, which made a surviving cue jump to the bottom when the cue below it ended).
     * [getCues] is only called as a single forward pass at parse time, so keeping state here is safe.
     */
    private val assignedLines = HashMap<Long, Float>()

    private fun positionCuesWithoutOverlap(cues: List<ActiveCue>): List<Cue> {
        if (cues.isEmpty()) {
            // Subtitle gap: reset the baseline so the next cue starts at the bottom again.
            assignedLines.clear()
            return emptyList()
        }

        // Free the lines of cues that have ended.
        val activeIds = cues.mapTo(HashSet()) { it.startTimeUs }
        assignedLines.keys.retainAll(activeIds)

        val occupiedLines = HashMap<Float, Boolean>()

        // Reserve the lines already held by cues with a sticky assignment.
        for (activeCue in cues) {
            val line = assignedLines[activeCue.startTimeUs] ?: continue
            for (i in 0 until activeCue.lineCount) {
                occupiedLines[line - i] = true
            }
        }

        // Assign a line to each new cue: lowest free block starting at the bottom (-2) moving up.
        for (activeCue in cues) {
            if (assignedLines.containsKey(activeCue.startTimeUs)) continue

            var linePosition = -2f
            while (true) {
                var hasSpace = true
                for (i in 0 until activeCue.lineCount) {
                    if (occupiedLines.containsKey(linePosition - i)) {
                        hasSpace = false
                        break
                    }
                }
                if (hasSpace) break
                linePosition -= 1
            }

            for (i in 0 until activeCue.lineCount) {
                occupiedLines[linePosition - i] = true
            }
            assignedLines[activeCue.startTimeUs] = linePosition
        }

        // Emit every active cue at its (now stable) line.
        return cues.map { activeCue ->
            activeCue.cue.buildUpon()
                .setLine(assignedLines.getValue(activeCue.startTimeUs), Cue.LINE_TYPE_NUMBER)
                .build()
        }
    }
}