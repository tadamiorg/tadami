package com.sf.tadami.source.model

/**
 * Canonical, translatable publication status for an anime.
 *
 * Extensions set one of these defined values instead of their own free-form site text, so the app
 * can translate the status. Defaults to [UNKNOWN] until an extension is updated to the new API.
 *
 * @since extensions-lib 1.5
 */
enum class SAnimeStatus {
    /** Not given by the site. */
    UNKNOWN,

    /** Still airing. */
    ONGOING,

    /** Finished airing. */
    COMPLETED,

    /** Paused, to be resumed. */
    ON_HIATUS,

    /** Stopped for good. */
    CANCELLED
}
