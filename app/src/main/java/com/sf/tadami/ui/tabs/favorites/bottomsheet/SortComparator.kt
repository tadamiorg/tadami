package com.sf.tadami.ui.tabs.favorites.bottomsheet

import com.sf.tadami.ui.components.data.FavoriteItem
import com.sf.tadami.ui.tabs.settings.screens.library.LibrarySort

fun sortComparator(sort: LibrarySort): (FavoriteItem, FavoriteItem) -> Int {
    return when (sort.sortType) {
        is LibrarySort.SortType.Alphabetical -> when (sort.isAscending) {
            true -> {
                { a1, a2 -> a2.anime.title.compareTo(a1.anime.title) }
            }
            false -> {
                { a1, a2 -> a1.anime.title.compareTo(a2.anime.title) }
            }
        }
        is LibrarySort.SortType.UnseenCount -> when (sort.isAscending) {
            true -> {
                { a1, a2 -> a2.anime.unseenEpisodes.compareTo(a1.anime.unseenEpisodes) }
            }
            false -> {
                { a1, a2 ->
                    when {
                        a1.anime.unseenEpisodes == 0L -> 1
                        a2.anime.unseenEpisodes == 0L -> -1
                        else -> a1.anime.unseenEpisodes.compareTo(a2.anime.unseenEpisodes)
                    }
                }
            }
        }
        is LibrarySort.SortType.EpisodeCount -> when (sort.isAscending) {
            true -> {
                { a1, a2 -> a2.anime.episodes.compareTo(a1.anime.episodes) }
            }
            false -> {
                { a1, a2 -> a1.anime.episodes.compareTo(a2.anime.episodes) }
            }
        }
    }
}
