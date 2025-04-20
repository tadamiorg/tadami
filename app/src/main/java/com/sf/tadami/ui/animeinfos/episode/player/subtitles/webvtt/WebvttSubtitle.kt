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

    private fun positionCuesWithoutOverlap(cues: List<ActiveCue>): List<Cue> {
        if (cues.isEmpty()) return emptyList()

        val result = mutableListOf<Cue>()
        val occupiedLines = mutableMapOf<Float, Int>() // Line number -> number of lines occupied

        // Start positioning from bottom of screen
        var currentLine = -2f

        for (activeCue in cues) {
            // Find first available position that has enough space for this cue
            var linePosition = currentLine

            while (true) {
                var hasSpace = true
                // Check if there's enough space for all lines of this cue
                for (i in 0 until activeCue.lineCount) {
                    if (occupiedLines.containsKey(linePosition - i)) {
                        hasSpace = false
                        break
                    }
                }

                if (hasSpace) {
                    break
                }
                linePosition -= 1
            }

            // Mark all lines used by this cue as occupied
            for (i in 0 until activeCue.lineCount) {
                occupiedLines[linePosition - i] = 1
            }

            // Create new cue with found position
            val adjustedCue = activeCue.cue.buildUpon()
                .setLine(linePosition, Cue.LINE_TYPE_NUMBER)
                .build()

            result.add(adjustedCue)

            // Update current line position for next cue
            currentLine = linePosition - activeCue.lineCount
        }

        return result
    }
}