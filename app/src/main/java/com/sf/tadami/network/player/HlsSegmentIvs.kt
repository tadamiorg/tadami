package com.sf.tadami.network.player

/**
 * Fixes HLS playlists whose AES-128 segments were encrypted with their encoder sequence number as
 * IV, after the host removed some of them (e.g. empty ones) without renumbering, as Kwik does.
 *
 * Without an explicit IV, players derive it from the segment position in the playlist, so every
 * segment after the gap decrypts with a wrong first block. That block holds the transport stream's
 * program table: playback starting or seeking past the gap never finds the tracks
 * ("Loading finished before preparation is complete").
 */
internal object HlsSegmentIvs {

    private const val KEY_TAG = "#EXT-X-KEY:"
    private const val MEDIA_SEQUENCE_TAG = "#EXT-X-MEDIA-SEQUENCE:"
    private val NUMBER = Regex("""\d+""")
    private val IV_ATTRIBUTE = Regex("""(^|,)\s*IV=""")

    /** The playlist with an explicit IV for each segment, or null when it doesn't need one. */
    fun fix(playlist: String): String? {
        if (!playlist.startsWith("#EXTM3U") || KEY_TAG !in playlist) return null
        val lines = playlist.lines()
        val mediaSequence = lines.firstOrNull { it.startsWith(MEDIA_SEQUENCE_TAG) }
            ?.substringAfter(':')?.trim()?.toLongOrNull() ?: 0L

        val segments = mutableListOf<Segment>()
        var key: String? = null
        lines.forEachIndexed { index, line ->
            when {
                line.startsWith(KEY_TAG) -> key = line
                line.isNotBlank() && !line.startsWith("#") -> segments += Segment(index, key, fileNumber(line))
            }
        }
        // Only when every segment's IV comes from its position, the IV of the others is right
        if (segments.isEmpty() || !segments.all { it.key.isImplicitAes() }) return null

        val numbers = segments.map { it.number ?: return null }
        // File names numbered like the sequence, increasing, with a gap: the encoder's numbering
        if (numbers.first() != mediaSequence) return null
        if (numbers.zipWithNext().any { (previous, next) -> next <= previous }) return null
        if (numbers.withIndex().all { (index, number) -> number == mediaSequence + index }) return null

        val fixed = lines.toMutableList()
        // From the end, so the indexes of the segments still to do stay valid
        segments.zip(numbers).asReversed().forEach { (segment, number) ->
            fixed.add(segment.line, "${segment.key},IV=0x${"%032x".format(number)}")
        }
        return fixed.joinToString("\n")
    }

    private fun String?.isImplicitAes(): Boolean {
        if (this == null) return false
        val attributes = substringAfter(':')
        return "METHOD=AES-128" in attributes && !IV_ATTRIBUTE.containsMatchIn(attributes)
    }

    // e.g. 5 for ".../segment-5-v1-a1.jpg"
    private fun fileNumber(uri: String): Long? {
        val name = uri.substringBefore('?').substringAfterLast('/')
        return NUMBER.find(name)?.value?.toLongOrNull()
    }

    private class Segment(val line: Int, val key: String?, val number: Long?)
}
