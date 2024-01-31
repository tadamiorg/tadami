package com.sf.tadami.extensions.model

import com.sf.tadami.domain.extensions.Extension

sealed interface LoadResult {
    data class Success(val extension: Extension.Installed) : LoadResult
    data object Error : LoadResult
}