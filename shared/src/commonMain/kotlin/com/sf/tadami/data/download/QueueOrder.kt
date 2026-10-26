package com.sf.tadami.data.download

/** New orders of the download queue, for [TadamiDownloadManager.reorderQueue]. */
object QueueOrder {

    /** [moved] first, in their queue order, then the others. Those not in [queue] are left out. */
    fun <T> toTop(queue: List<T>, moved: Collection<T>): List<T> {
        val (first, others) = queue.partition { it in moved }
        return first + others
    }

    /** The others first, then [moved] in their queue order. Those not in [queue] are left out. */
    fun <T> toBottom(queue: List<T>, moved: Collection<T>): List<T> {
        val (last, others) = queue.partition { it in moved }
        return others + last
    }

    /**
     * The [running] downloads to stop so that [moved], just moved to the top of [queue], starts at
     * once with [parallel] slots: the one of its source (one download per source), then the lowest
     * in [queue] until a slot is free. Empty when [moved] runs already or can start as it is.
     */
    fun <T> toPreempt(
        queue: List<T>,
        running: Collection<T>,
        parallel: Int,
        moved: T,
        sourceOf: (T) -> Any,
    ): List<T> {
        if (moved in running) return emptyList()
        val source = sourceOf(moved)
        val preempted = running.filter { sourceOf(it) == source }.toMutableList()
        var left = running.size - preempted.size
        for (download in queue.asReversed()) {
            if (left < parallel) break
            if (download !in running || download in preempted) continue
            preempted += download
            left--
        }
        return preempted
    }
}
