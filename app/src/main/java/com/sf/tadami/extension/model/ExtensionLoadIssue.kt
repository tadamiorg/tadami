package com.sf.tadami.extension.model

import androidx.annotation.StringRes
import com.sf.tadami.R

/**
 * Why an installed extension can't be used. It stays listed, with this reason, but its sources
 * aren't loaded, so nothing can run its code.
 */
enum class ExtensionLoadIssue(
    /** Short label for lists. */
    @StringRes val labelRes: Int,
    /** What is wrong and what fixes it. */
    @StringRes val messageRes: Int,
) {
    /** Built against an older extensions lib than this app accepts: the extension needs an update. */
    LIB_TOO_OLD(R.string.ext_issue_lib_too_old, R.string.ext_issue_lib_too_old_message),

    /** Built against a newer extensions lib than this app accepts: the app needs an update. */
    LIB_TOO_NEW(R.string.ext_issue_lib_too_new, R.string.ext_issue_lib_too_new_message),

    /** Its version name isn't "major.minor.code", so its lib version is unknown. */
    INVALID_VERSION(R.string.ext_issue_invalid_version, R.string.ext_issue_invalid_version_message),

    /** Its sources couldn't be found or instantiated. */
    LOAD_FAILED(R.string.ext_issue_load_failed, R.string.ext_issue_load_failed_message),

    /** Signed with a key that isn't the official one nor trusted by the user: none of it is loaded. */
    UNTRUSTED(R.string.ext_issue_untrusted, R.string.ext_issue_untrusted_message),

    /** Its sources took too long to build: listed as is until they are, if ever. */
    TIMED_OUT(R.string.ext_issue_timed_out, R.string.ext_issue_timed_out_message),

    /** A crash of the app came from it: not loaded again until the user retries it, or it's replaced. */
    QUARANTINED(R.string.ext_issue_quarantined, R.string.ext_issue_quarantined_message),

    /** Not loaded: the app started in safe mode after crashing at startup, until the user loads them again. */
    SAFE_MODE(R.string.ext_issue_safe_mode, R.string.ext_issue_safe_mode_message),

    /**
     * A source of another extension has the ID of one of its sources, see
     * [com.sf.tadami.domain.extensions.Extension.Installed.sourceIdConflict], which names it: its
     * sources aren't registered while that one is installed.
     */
    SOURCE_ID_CONFLICT(R.string.ext_issue_source_id_conflict, R.string.ext_issue_source_id_conflict_message),
}
