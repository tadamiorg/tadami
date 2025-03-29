package com.sf.tadami.ui.animeinfos.episode.player.subtitles

import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
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
        return if (MimeTypes.TEXT_VTT == format.sampleMimeType) {
            CustomWebvttParser()
        } else {
            defaultFactory.create(format)
        }
    }
}