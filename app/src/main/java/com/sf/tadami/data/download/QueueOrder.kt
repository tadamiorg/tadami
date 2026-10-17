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
}
