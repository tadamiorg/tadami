package com.sf.tadami.extension.model

import com.sf.tadami.domain.extensions.Extension

sealed interface LoadResult {
    /** An installed extension, unusable when [Extension.Installed.loadIssue] is set. */
    data class Success(val extension: Extension.Installed) : LoadResult

    /** Not an extension that can even be listed. */
    data object Error : LoadResult
}