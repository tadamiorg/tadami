package com.sf.tadami.ui.tabs.settings.screens.library

data class LibrarySort(
    val flags: Long
) {
    val isAscending: Boolean
        get() = flags and SORT_DIRECTION_MASK == SORT_DIRECTION

    val sortType : SortType
        get() = SortType.valueOf(flags)

    sealed class SortType(
        val flag: Long,
    ) {
        object Alphabetical : SortType(ALPHABETICALLY)
        object UnseenCount : SortType(UNSEEN_COUNT)
        object EpisodeCount : SortType(EPISODE_COUNT)

        companion object {
            private val types = setOf(Alphabetical, UnseenCount, EpisodeCount)
            fun valueOf(flags: Long): SortType {
                return types.find { type -> type.flag == flags and SORT_TYPE_MASK } ?: Alphabetical
            }
        }
    }
    companion object {
        const val DEFAULT_SORT = 0x000000001L

        const val ALPHABETICALLY = 0x00000001L
        const val UNSEEN_COUNT = 0x00000002L
        const val EPISODE_COUNT = 0x00000004L
        const val SORT_TYPE_MASK = 0x7FFFFFFFL

        const val SORT_DIRECTION = 0x80000000L
        const val SORT_DIRECTION_MASK = 0x80000000L
    }
}