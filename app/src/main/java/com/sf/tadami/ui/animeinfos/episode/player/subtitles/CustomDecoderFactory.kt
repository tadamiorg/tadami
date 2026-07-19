package com.sf.tadami.ui.animeinfos.episode.player.subtitles

import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.Consumer
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import androidx.media3.extractor.text.SubtitleParser
import com.sf.tadami.ui.animeinfos.episode.player.subtitles.webvtt.CustomWebvttParser

@UnstableApi
class CustomSubtitleParserFactory : SubtitleParser.Factory {
    private val defaultFactory = DefaultSubtitleParserFactory()

    override fun supportsFormat(format: Format): Boolean {
       return defaultFactory.supportsFormat(format)
    }

    override fun getCueReplacementBehavior(format: Format): Int {
        return defaultFactory.getCueReplacementBehavior(format)
    }

    override fun create(format: Format): SubtitleParser {
        // The declared mimeType can be wrong — some hosts serve WebVTT under a `.srt` URL (which the extractor
        // then tags application/x-subrip). We can't re-sniff at extraction time because those hosts 403 requests
        // without the source headers, but ExoPlayer has already fetched the real bytes here (with the headers),
        // so sniff the content at parse time and route actual WebVTT to our custom parser regardless of the label.
        return ContentSniffingSubtitleParser(format, defaultFactory)
    }
}

/**
 * Chooses the concrete [SubtitleParser] from the actual first bytes rather than the (possibly wrong) declared
 * mimeType: WebVTT content → [CustomWebvttParser]; anything else → the media3 default for [format].
 */
@UnstableApi
private class ContentSniffingSubtitleParser(
    private val format: Format,
    private val defaultFactory: DefaultSubtitleParserFactory,
) : SubtitleParser {

    private fun delegateFor(data: ByteArray, offset: Int, length: Int): SubtitleParser {
        val peek = String(data, offset, minOf(length, 32), Charsets.UTF_8)
            .trimStart('﻿', ' ', '\t', '\r', '\n') // strip a UTF-8 BOM and leading whitespace if present
        val isWebvtt = peek.startsWith("WEBVTT") || MimeTypes.TEXT_VTT == format.sampleMimeType
        return if (isWebvtt) CustomWebvttParser() else defaultFactory.create(format)
    }

    override fun getCueReplacementBehavior(): Int {
        return defaultFactory.getCueReplacementBehavior(format)
    }

    override fun parse(
        data: ByteArray,
        offset: Int,
        length: Int,
        outputOptions: SubtitleParser.OutputOptions,
        output: Consumer<CuesWithTiming>,
    ) {
        delegateFor(data, offset, length).parse(data, offset, length, outputOptions, output)
    }
}
