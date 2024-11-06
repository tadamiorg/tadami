package com.sf.tadami.data.backup

import androidx.annotation.StringRes
import com.sf.tadami.R
import kotlinx.collections.immutable.persistentListOf

data class BackupOptions(
    val libraryEntries: Boolean = true,
    val categories: Boolean = true,
    val episodes: Boolean = true,
    val history: Boolean = true,
    val appSettings: Boolean = true,
    val sourcesSettings: Boolean = false,
) {

    fun asBooleanArray() = booleanArrayOf(
        libraryEntries,
        categories,
        episodes,
        history,
        appSettings,
        sourcesSettings,
    )

    fun canCreate() = libraryEntries || categories || appSettings || sourcesSettings

    companion object {
        val libraryOptions = persistentListOf(
            Entry(
                label = R.string.library_entries,
                getter = BackupOptions::libraryEntries,
                setter = { options, enabled -> options.copy(libraryEntries = enabled) },
            ),
            Entry(
                label = R.string.label_episodes,
                getter = BackupOptions::episodes,
                setter = { options, enabled -> options.copy(episodes = enabled) },
                enabled = { it.libraryEntries },
            ),
            Entry(
                label = R.string.label_history,
                getter = BackupOptions::history,
                setter = { options, enabled -> options.copy(history = enabled) },
                enabled = { it.libraryEntries },
            )
        )

        val settingsOptions = persistentListOf(
            Entry(
                label = R.string.app_settings,
                getter = BackupOptions::appSettings,
                setter = { options, enabled -> options.copy(appSettings = enabled) },
            ),
            Entry(
                label = R.string.sources_settings,
                getter = BackupOptions::sourcesSettings,
                setter = { options, enabled -> options.copy(sourcesSettings = enabled) },
            )
        )

        fun fromBooleanArray(array: BooleanArray) = BackupOptions(
            libraryEntries = array[0],
            categories = array[1],
            episodes = array[2],
            history = array[3],
            appSettings = array[4],
            sourcesSettings = array[5],
        )
    }

    data class Entry(
        @StringRes
        val label: Int,
        val getter: (BackupOptions) -> Boolean,
        val setter: (BackupOptions, Boolean) -> BackupOptions,
        val enabled: (BackupOptions) -> Boolean = { true },
    )
}
