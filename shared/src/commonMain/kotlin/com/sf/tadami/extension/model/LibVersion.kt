package com.sf.tadami.extension.model

/**
 * Extension lib version, the "major.minor" part of an extension version name ("major.minor.code").
 * Compared numerically part by part, so 1.10 is greater than 1.9.
 */
data class LibVersion(val major: Int, val minor: Int) : Comparable<LibVersion> {

    override fun compareTo(other: LibVersion): Int {
        return if (major != other.major) major.compareTo(other.major) else minor.compareTo(other.minor)
    }

    override fun toString(): String = "$major.$minor"

    companion object {
        /**
         * Parses the lib version of an extension version name ("major.minor.code").
         * Returns null when the version name is malformed.
         */
        fun fromVersionName(versionName: String): LibVersion? {
            val parts = versionName.substringBeforeLast('.', missingDelimiterValue = "").split('.')
            if (parts.size != 2) return null
            val major = parts[0].toIntOrNull() ?: return null
            val minor = parts[1].toIntOrNull() ?: return null
            return LibVersion(major, minor)
        }
    }
}
