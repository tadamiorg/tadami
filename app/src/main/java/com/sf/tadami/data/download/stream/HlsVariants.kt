package com.sf.tadami.data.download.stream

/** Picks what to download from an HLS multivariant playlist. */
object HlsVariants {

    /** A variant: its index in the playlist, its resolution and bitrate, unknown values as 0. */
    data class Variant(
        val index: Int,
        val height: Int,
        val bandwidth: Int,
        /** Its audio is a separate rendition: another file to download and merge */
        val demuxedAudio: Boolean,
    )

    /** An audio rendition of the variant's group, with a URI of its own. */
    data class AudioRendition(
        val index: Int,
        val isDefault: Boolean,
    )

    private val RESOLUTION = Regex("""(?<!\d)(\d{3,4})\s*[xX×]\s*(\d{3,4})(?!\d)""")
    private val HEIGHT = Regex("""(?<!\d)(\d{3,4})\s*[pP](?![a-zA-Z])""")
    private val COMMON_HEIGHTS = setOf(144, 240, 360, 480, 540, 576, 720, 1080, 1440, 2160)
    private val NUMBER = Regex("""(?<!\d)(\d{3,4})(?!\d)""")

    /** The height a quality label asks for: "1080p", "720", "1920x1080", "FHD 1080p"; null when it states none. */
    fun targetHeight(quality: String): Int? {
        RESOLUTION.find(quality)?.let { return it.groupValues[2].toInt() }
        HEIGHT.find(quality)?.let { return it.groupValues[1].toInt() }
        return NUMBER.findAll(quality).map { it.groupValues[1].toInt() }.firstOrNull { it in COMMON_HEIGHTS }
    }

    /**
     * The variant closest to the stream's quality label when it states a height, else the highest.
     * Variants with their audio muxed in come first: one file, nothing to merge. With a [maxHeight],
     * only the variants it allows, see [withinMaxHeight].
     */
    fun chooseVariant(variants: List<Variant>, quality: String, maxHeight: Int? = null): Variant? {
        val allowed = withinMaxHeight(variants, maxHeight)
        val candidates = allowed.filter { !it.demuxedAudio }.ifEmpty { allowed }
        if (candidates.isEmpty()) return null
        val target = targetHeight(quality)
        val best = compareBy<Variant> { it.height }.thenBy { it.bandwidth }
        if (target == null || candidates.all { it.height <= 0 }) return candidates.maxWith(best)
        return candidates
            .filter { it.height > 0 }
            .minWith(compareBy<Variant> { kotlin.math.abs(it.height - target) }.then(best.reversed()))
    }

    /**
     * The variants at or below [maxHeight], else those of the lowest height above it. All of them
     * without a maximum, or when no variant states its resolution.
     */
    private fun withinMaxHeight(variants: List<Variant>, maxHeight: Int?): List<Variant> {
        val known = variants.filter { it.height > 0 }
        if (maxHeight == null || known.isEmpty()) return variants
        val fitting = known.filter { it.height <= maxHeight }
        if (fitting.isNotEmpty()) return fitting
        val lowest = known.minOf { it.height }
        return known.filter { it.height == lowest }
    }

    /** The rendition to download for a demuxed variant: the default one, else the first. */
    fun chooseAudio(renditions: List<AudioRendition>): AudioRendition? {
        return renditions.firstOrNull { it.isDefault } ?: renditions.firstOrNull()
    }
}
