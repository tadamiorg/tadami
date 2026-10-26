package com.sf.tadami.source.model

/**
 * Canonical, translatable publication status for an anime.
 *
 * Extensions set one of these defined values instead of their own free-form site text, so the app
 * can translate the status. Defaults to [UNKNOWN] until an extension is updated to the new API.
 */
enum class SAnimeStatus {
    UNKNOWN,
    ONGOING,
    COMPLETED,
    ON_HIATUS,
    CANCELLED
}
