package com.sf.tadami.extension.model

/**
 * Why the sources of an extension aren't registered, listed as [ExtensionLoadIssue.SOURCE_ID_CONFLICT]:
 * the app keys the anime, the preferences and the downloads of a source by its ID, so two sources
 * with the same ID would mix them up.
 */
sealed interface SourceIdConflict {

    /** Two of its sources have the same ID. */
    data object Duplicate : SourceIdConflict

    /**
     * One of its sources has an ID of an official source, see
     * [com.sf.tadami.extension.util.OFFICIAL_SOURCE_IDS], without the official signing key.
     */
    data object Reserved : SourceIdConflict

    /** A source of the extension [ownerPkgName], named [ownerName], has the ID of one of its sources. */
    data class TakenBy(val ownerPkgName: String, val ownerName: String) : SourceIdConflict
}
