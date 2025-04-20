package com.sf.tadami.ui.animeinfos.episode.player.subtitles.webvtt


import android.text.TextUtils
import androidx.media3.common.Format
import androidx.media3.common.Format.CueReplacementBehavior
import androidx.media3.common.ParserException
import androidx.media3.common.util.Consumer
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.LegacySubtitleUtil
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.text.SubtitleParser.OutputOptions
import androidx.media3.extractor.text.webvtt.WebvttCssStyle
import androidx.media3.extractor.text.webvtt.WebvttCueInfo
import androidx.media3.extractor.text.webvtt.WebvttCueParser
import androidx.media3.extractor.text.webvtt.WebvttParserUtil

@UnstableApi
class CustomWebvttParser : SubtitleParser {
    private val parsableWebvttData = ParsableByteArray()
    private val cssParser = WebvttCssParser()

    override fun getCueReplacementBehavior(): @CueReplacementBehavior Int {
        return CUE_REPLACEMENT_BEHAVIOR
    }

    override fun parse(
        data: ByteArray,
        offset: Int,
        length: Int,
        outputOptions: OutputOptions,
        output: Consumer<CuesWithTiming>
    ) {
        parsableWebvttData.reset(data,  /* limit= */offset + length)
        parsableWebvttData.position = offset
        val definedStyles: MutableList<WebvttCssStyle> = ArrayList()

        // Validate the first line of the header, and skip the remainder.
        try {
            WebvttParserUtil.validateWebvttHeaderLine(parsableWebvttData)
        } catch (e: ParserException) {
            throw IllegalArgumentException(e)
        }
        while (!TextUtils.isEmpty(parsableWebvttData.readLine())) {
        }

        var event: Int
        val cueInfos: MutableList<WebvttCueInfo> = ArrayList()
        while ((getNextEvent(parsableWebvttData)
                .also { event = it }) != EVENT_END_OF_FILE
        ) {
            if (event == EVENT_COMMENT) {
                skipComment(parsableWebvttData)
            } else if (event == EVENT_STYLE_BLOCK) {
                require(cueInfos.isEmpty()) { "A style block was found after the first cue." }
                parsableWebvttData.readLine() // Consume the "STYLE" header.
                definedStyles.addAll(cssParser.parseBlock(parsableWebvttData))
            } else if (event == EVENT_CUE) {
                val cueInfo = WebvttCueParser.parseCue(parsableWebvttData, definedStyles)
                if (cueInfo != null) {
                    cueInfos.add(cueInfo)
                }
            }
        }
        val subtitle = WebvttSubtitle(cueInfos)
        LegacySubtitleUtil.toCuesWithTiming(subtitle, outputOptions, output)
    }

    companion object {
        /**
         * The [CueReplacementBehavior] for consecutive [CuesWithTiming] emitted by this
         * implementation.
         */
        const val CUE_REPLACEMENT_BEHAVIOR: @CueReplacementBehavior Int =
            Format.CUE_REPLACEMENT_BEHAVIOR_MERGE

        private const val EVENT_NONE = -1
        private const val EVENT_END_OF_FILE = 0
        private const val EVENT_COMMENT = 1
        private const val EVENT_STYLE_BLOCK = 2
        private const val EVENT_CUE = 3

        private const val COMMENT_START = "NOTE"
        private const val STYLE_START = "STYLE"

        /**
         * Positions the input right before the next event, and returns the kind of event found. Does not
         * consume any data from such event, if any.
         *
         * @return The kind of event found.
         */
        private fun getNextEvent(parsableWebvttData: ParsableByteArray): Int {
            var foundEvent: Int = EVENT_NONE
            var currentInputPosition = 0
            while (foundEvent == EVENT_NONE) {
                currentInputPosition = parsableWebvttData.position
                val line = parsableWebvttData.readLine()
                foundEvent = if (line == null) {
                    EVENT_END_OF_FILE
                } else if (CustomWebvttParser.STYLE_START == line) {
                    EVENT_STYLE_BLOCK
                } else if (line.startsWith(CustomWebvttParser.COMMENT_START)) {
                    EVENT_COMMENT
                } else {
                    EVENT_CUE
                }
            }
            parsableWebvttData.position = currentInputPosition
            return foundEvent
        }

        private fun skipComment(parsableWebvttData: ParsableByteArray) {
            while (!TextUtils.isEmpty(parsableWebvttData.readLine())) {
            }
        }
    }
}
