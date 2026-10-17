package com.sf.tadami.data.download

import org.junit.Assert.assertEquals
import org.junit.Test

class QueueOrderTest {

    private val queue = listOf("a", "b", "c", "d", "e")

    @Test
    fun movedToTopKeepTheirOrder() {
        assertEquals(listOf("d", "a", "b", "c", "e"), QueueOrder.toTop(queue, listOf("d")))
        assertEquals(listOf("b", "d", "a", "c", "e"), QueueOrder.toTop(queue, setOf("d", "b")))
    }

    @Test
    fun movedToBottomKeepTheirOrder() {
        assertEquals(listOf("b", "c", "d", "e", "a"), QueueOrder.toBottom(queue, listOf("a")))
        assertEquals(listOf("a", "c", "e", "b", "d"), QueueOrder.toBottom(queue, listOf("d", "b")))
    }

    @Test
    fun itemsNoLongerQueuedAreLeftOut() {
        // Downloaded meanwhile: the queue is the source of truth
        assertEquals(queue, QueueOrder.toTop(queue, listOf("z")))
        assertEquals(listOf("a", "b", "d", "e", "c"), QueueOrder.toBottom(queue, listOf("z", "c")))
    }

    @Test
    fun alreadyInPlaceChangesNothing() {
        assertEquals(queue, QueueOrder.toTop(queue, listOf("a")))
        assertEquals(queue, QueueOrder.toBottom(queue, listOf("e")))
    }
}
