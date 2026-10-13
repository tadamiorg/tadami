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
}
