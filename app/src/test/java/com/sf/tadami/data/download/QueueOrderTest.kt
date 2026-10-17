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

    // "<episode>:<source>"
    private fun preempt(queue: List<String>, running: List<String>, parallel: Int) =
        QueueOrder.toPreempt(queue, running, parallel, queue.first()) { it.substringAfter(':') }

    @Test
    fun slotsFullStopsTheLowestRunning() {
        // Moved "e" to the top, "a" and "b" run in the 2 slots
        val queue = listOf("e:3", "a:1", "b:2", "c:1", "d:2")
        assertEquals(listOf("b:2"), preempt(queue, running = listOf("a:1", "b:2"), parallel = 2))
        // Running order doesn't matter, the queue's does
        assertEquals(listOf("b:2"), preempt(queue, running = listOf("b:2", "a:1"), parallel = 2))
    }

    @Test
    fun itsSourceBusyStopsTheDownloadOfItsSource() {
        // One download per source: "a" of the same source stops, even ranked above "b"
        val queue = listOf("e:1", "a:1", "b:2")
        assertEquals(listOf("a:1"), preempt(queue, running = listOf("a:1", "b:2"), parallel = 2))
        assertEquals(listOf("a:1"), preempt(queue, running = listOf("a:1", "b:2"), parallel = 3))
    }

    @Test
    fun aFreeSlotOfAFreeSourceStopsNothing() {
        val queue = listOf("e:3", "a:1", "b:2")
        assertEquals(emptyList<String>(), preempt(queue, running = listOf("a:1", "b:2"), parallel = 3))
        assertEquals(emptyList<String>(), preempt(queue, running = emptyList(), parallel = 1))
    }

    @Test
    fun alreadyRunningStopsNothing() {
        val queue = listOf("a:1", "b:2")
        assertEquals(emptyList<String>(), preempt(queue, running = listOf("a:1", "b:2"), parallel = 2))
    }

    @Test
    fun fewerSlotsThanRunningStopsEnoughForOne() {
        // The setting went from 3 to 1 while 3 ran: all of them stop for the moved one
        val queue = listOf("e:4", "a:1", "b:2", "c:3")
        assertEquals(listOf("c:3", "b:2", "a:1"), preempt(queue, running = listOf("a:1", "b:2", "c:3"), parallel = 1))
        // Its source's download counts as one of them
        assertEquals(listOf("a:1", "c:3"), preempt(listOf("e:1", "a:1", "b:2", "c:3"), listOf("a:1", "b:2", "c:3"), parallel = 2))
    }
}
