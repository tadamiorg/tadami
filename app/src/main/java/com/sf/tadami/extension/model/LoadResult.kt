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
    ) : LoadResult

    enum class Reason {
        /** Package isn't installed, or no longer declares the extension feature. */
        NOT_FOUND,

        /** Missing versionName, missing metadata, unreadable manifest. */
        MALFORMED,

        /** Extension lib version outside the range this app supports. */
        INCOMPATIBLE_API,

        /** PathClassLoader failed, or the source class threw while being constructed. */
        CLASSLOAD_FAILED,

        /** The source constructor didn't return within the load timeout. */
        TIMED_OUT,

        /** Skipped on purpose: it hung or killed the process during the previous launch. */
        QUARANTINED,
    }
}
