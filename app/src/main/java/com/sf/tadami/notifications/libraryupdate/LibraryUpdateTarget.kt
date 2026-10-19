package com.sf.tadami.notifications.libraryupdate

/** What a library update fetches, as Mihon's library update targets */
enum class LibraryUpdateTarget {
    // The new episodes, the regular library update
    EPISODES,

    // The details and the cover of each anime, not the episodes
    DETAILS;

    companion object {
        /** The target stored in the worker's input, the regular update when there's none */
        fun of(name: String?): LibraryUpdateTarget = entries.find { it.name == name } ?: EPISODES
    }
}

/** What the update does for each anime: fetches its episodes, its details, and downloads its cover again */
internal data class LibraryUpdateParts(
    val episodes: Boolean,
    val details: Boolean,
    // Even at the same URL: the source can serve a new image there
    val refreshCovers: Boolean,
)

internal fun LibraryUpdateTarget.parts(): LibraryUpdateParts = when (this) {
    LibraryUpdateTarget.EPISODES -> LibraryUpdateParts(episodes = true, details = false, refreshCovers = false)
    LibraryUpdateTarget.DETAILS -> LibraryUpdateParts(episodes = false, details = true, refreshCovers = true)
}
