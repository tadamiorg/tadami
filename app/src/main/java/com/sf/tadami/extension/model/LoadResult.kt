package com.sf.tadami.extension.model

import com.sf.tadami.domain.extensions.Extension

sealed interface LoadResult {
    data class Success(val extension: Extension.Installed) : LoadResult

    /**
     * A package that looked like an extension but couldn't be turned into one. [pkgName] and
     * [reason] are kept so the Extensions tab can tell the user what happened instead of the
     * extension silently disappearing from the list.
     */
    data class Error(
        val pkgName: String,
        val reason: Reason,
        val extName: String? = null,
        val throwable: Throwable? = null,
        /**
         * Signing certificate of the rejected extension, present only for [Reason.UNTRUSTED] —
         * it's what the "trust this signature" prompt needs to show and then persist.
         */
        val fingerprint: String? = null,
    ) : LoadResult

    enum class Reason {
        /** No file for this package, or it no longer declares the extension feature. */
        NOT_FOUND,

        /** Missing versionName, missing metadata, unreadable manifest, package name mismatch. */
        MALFORMED,

        /** Extension lib version outside the range this app supports. */
        INCOMPATIBLE_API,

        /**
         * Signed by a certificate that is neither the official one nor one the user has accepted.
         * Nothing from the extension has been loaded — the check happens before its code is touched.
         */
        UNTRUSTED,

        /** PathClassLoader failed, or the source class threw while being constructed. */
        CLASSLOAD_FAILED,

        /** The source constructor didn't return within the load timeout. */
        TIMED_OUT,

        /** Skipped on purpose: it hung or killed the process during the previous launch. */
        QUARANTINED,
    }
}
