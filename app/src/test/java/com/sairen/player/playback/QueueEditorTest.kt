package com.sairen.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueueEditorTest {

    @Test
    fun `moveUp swaps with the previous entry`() {
        val plan = QueueEditor.moveUp(index = 2, currentIndex = 0, size = 4)
        assertEquals(QueueEditor.MovePlan(from = 2, to = 1, newCurrentIndex = 0), plan)
    }

    @Test
    fun `moveUp is rejected for the first entry`() {
        assertNull(QueueEditor.moveUp(index = 0, currentIndex = 1, size = 4))
    }

    @Test
    fun `moveDown is rejected for the last entry`() {
        assertNull(QueueEditor.moveDown(index = 3, currentIndex = 1, size = 4))
    }

    @Test
    fun `moving the current entry keeps the current index on it`() {
        val plan = QueueEditor.moveDown(index = 1, currentIndex = 1, size = 4)!!
        assertEquals(1, plan.from)
        assertEquals(2, plan.to)
        assertEquals(2, plan.newCurrentIndex)
    }

    @Test
    fun `moving an entry across the current one adjusts the current index`() {
        // Moving index 0 below the current index 1 pushes the current track up one slot.
        val plan = QueueEditor.moveDown(index = 0, currentIndex = 1, size = 3)!!
        assertEquals(QueueEditor.MovePlan(from = 0, to = 1, newCurrentIndex = 0), plan)
    }

    @Test
    fun `playNext places a later entry directly after the current one`() {
        val plan = QueueEditor.playNext(index = 4, currentIndex = 1, size = 6)!!
        assertEquals(4, plan.from)
        assertEquals(2, plan.to)
        assertEquals(1, plan.newCurrentIndex)
    }

    @Test
    fun `playNext places an earlier entry directly after the current one and fixes the current index`() {
        // Removing index precedes the current item, so both shift one slot towards 0.
        val plan = QueueEditor.playNext(index = 0, currentIndex = 2, size = 5)!!
        assertEquals(0, plan.from)
        assertEquals(2, plan.to)
        assertEquals(1, plan.newCurrentIndex)
    }

    @Test
    fun `playNext is a no-op for the current entry and the one already next`() {
        assertNull(QueueEditor.playNext(index = 2, currentIndex = 2, size = 5))
        assertNull(QueueEditor.playNext(index = 3, currentIndex = 2, size = 5))
    }

    @Test
    fun `removing an entry before the current one shifts the current index`() {
        val plan = QueueEditor.removeAt(index = 0, currentIndex = 2, size = 4)!!
        assertEquals(0, plan.index)
        assertEquals(1, plan.newCurrentIndex)
        assertEquals(3, plan.newQueueSize)
    }

    @Test
    fun `removing the currently playing entry is refused`() {
        assertNull(QueueEditor.removeAt(index = 2, currentIndex = 2, size = 4))
    }

    @Test
    fun `the last remaining entry cannot be removed`() {
        assertNull(QueueEditor.removeAt(index = 0, currentIndex = 0, size = 1))
    }

    @Test
    fun `play next insertion lands right after the current item`() {
        assertEquals(2, QueueEditor.insertionIndexForPlayNext(currentIndex = 1, size = 5))
        assertEquals(1, QueueEditor.insertionIndexForPlayNext(currentIndex = 0, size = 5))
        assertEquals(3, QueueEditor.insertionIndexForPlayNext(currentIndex = 2, size = 3))
        assertEquals(0, QueueEditor.insertionIndexForPlayNext(currentIndex = -1, size = 0))
    }
}
