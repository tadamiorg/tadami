package com.sf.tadami.ui.discover.migrate.dialog

import androidx.annotation.StringRes
import com.sf.tadami.R


data class MigrationFlag(
    val flag: Int,
    val isDefaultSelected: Boolean,
    @StringRes val titleId: Int,
) {
    companion object {
        fun create(flag: Int, isDefaultSelected : Boolean = false, @StringRes titleId: Int): MigrationFlag {
            return MigrationFlag(
                flag = flag,
                isDefaultSelected = isDefaultSelected,
                titleId = titleId,
            )
        }
    }
}

object MigrationFlags {

    private const val EPISODES = 0x00000001
    private const val DELETE_DOWNLOADED = 0x00000010

    fun hasEpisodes(value: Int): Boolean {
        return value and EPISODES != 0
    }

    fun hasDeleteDownloaded(value: Int): Boolean {
        return value and DELETE_DOWNLOADED != 0
    }

    fun getFlags() : List<MigrationFlag>{
        val flags = mutableListOf<MigrationFlag>()
        flags += MigrationFlag.create(EPISODES, true, R.string.label_episodes)
        flags += MigrationFlag.create(DELETE_DOWNLOADED, false, R.string.delete_downloaded)
        return flags
    }

    fun getSelectedFlagsBitMap(
        selectedFlags: List<Boolean>,
        flags: List<MigrationFlag>,
    ): Int {
        return selectedFlags
            .zip(flags)
            .filter { (isSelected, _) -> isSelected }
            .map { (_, flag) -> flag.flag }
            .reduceOrNull { acc, mask -> acc or mask } ?: 0
    }
}