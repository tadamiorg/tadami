package com.sf.tadami.data.backup

import androidx.annotation.StringRes
import com.sf.tadami.R
import kotlinx.collections.immutable.persistentListOf

data class RestoreOptions(
    val libraryEntries: Boolean = true,
    val appSettings: Boolean = true,
    val sourcesSettings: Boolean = false,
) {

    fun asBooleanArray() = booleanArrayOf(
        libraryEntries,
        appSettings,
        sourcesSettings,
    )

    fun canRestore() = libraryEntries || appSettings || sourcesSettings

    companion object {
        val options = persistentListOf(
            Entry(
                label = R.string.library_entries,
                getter = RestoreOptions::libraryEntries,
                setter = { options, enabled -> options.copy(libraryEntries = enabled) },
            ),
            Entry(
                label = R.string.app_settings,
                getter = RestoreOptions::appSettings,
                setter = { options, enabled -> options.copy(appSettings = enabled) },
            ),
            Entry(
                label = R.string.sources_settings,
                getter = RestoreOptions::sourcesSettings,
                setter = { options, enabled -> options.copy(sourcesSettings = enabled) },
            )
        )

        fun fromBooleanArray(array: BooleanArray) = RestoreOptions(
            libraryEntries = array[0],
            appSettings = array[1],
            sourcesSettings = array[2],
        )
    }

    data class Entry(
        @StringRes
        val label: Int,
        val getter: (RestoreOptions) -> Boolean,
        val setter: (RestoreOptions, Boolean) -> RestoreOptions,
    )
}