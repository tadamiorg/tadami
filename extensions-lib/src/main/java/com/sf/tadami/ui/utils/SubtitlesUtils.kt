package com.sf.tadami.ui.utils

/**
 * The subtitle formats the app knows.
 *
 * @since extensions-lib 1.5
 */
object SubtitleFormats {
    /**
     * The MIME type of a subtitle of an unknown format.
     *
     * @since extensions-lib 1.5
     */
    val unknownFormat : String  = throw Exception("Stub !")
}

/**
 * The MIME type of the subtitle at [subtitleUrl], for [com.sf.tadami.source.model.Track.SubtitleTrack.mimeType]:
 * from its extension, or else from the start of its content, [SubtitleFormats.unknownFormat] when
 * neither tells.
 *
 * @since extensions-lib 1.5
 */
suspend fun detectSubtitleFormat(subtitleUrl: String): String = throw Exception("Stub !")
