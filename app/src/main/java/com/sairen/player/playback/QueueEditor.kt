package com.sairen.player.playback

/**
 * Pure index maths for queue edits.
 *
 * Media3's `Player.moveMediaItem(from, to)` removes the item first and then
 * inserts it, so moving an item that sits *before* the current item also shifts
 * the current index. All of that bookkeeping lives here (and is unit tested)
 * instead of being scattered through UI code.
 */
object QueueEditor {

    data class MovePlan(val from: Int, val to: Int, val newCurrentIndex: Int)

    data class RemovePlan(val index: Int, val newCurrentIndex: Int, val newQueueSize: Int)

    /** Swaps the entry at [index] with the one above it. */
    fun moveUp(index: Int, currentIndex: Int, size: Int): MovePlan? {
        if (size < 2 || index <= 0 || index >= size) return null
        return buildPlan(from = index, to = index - 1, currentIndex = currentIndex)
    }

    /** Swaps the entry at [index] with the one below it. */
    fun moveDown(index: Int, currentIndex: Int, size: Int): MovePlan? {
        if (size < 2 || index < 0 || index >= size - 1) return null
        return buildPlan(from = index, to = index + 1, currentIndex = currentIndex)
    }

    /** Computes the move that puts the entry at [index] directly after the current one. */
    fun playNext(index: Int, currentIndex: Int, size: Int): MovePlan? {
        if (index < 0 || index >= size || currentIndex < 0 || currentIndex >= size) return null
        if (index == currentIndex) return null
        if (index == currentIndex + 1) return null
        // Removing an earlier item shifts everything after it one slot towards 0.
        return if (index < currentIndex) {
            MovePlan(from = index, to = currentIndex, newCurrentIndex = currentIndex - 1)
        } else {
            MovePlan(from = index, to = currentIndex + 1, newCurrentIndex = currentIndex)
        }
    }

    /**
     * Removes a queue entry. Removing the currently playing item is rejected:
     * Media3 would silently start the next track, which reads as a bug to users.
     */
    fun removeAt(index: Int, currentIndex: Int, size: Int): RemovePlan? {
        if (index < 0 || index >= size || size < 2) return null
        if (index == currentIndex) return null
        val newCurrent = if (index < currentIndex) currentIndex - 1 else currentIndex
        return RemovePlan(index = index, newCurrentIndex = newCurrent, newQueueSize = size - 1)
    }

    /**
     * Index at which [Track]-level "play next" should insert into a player that
     * already has a current item: always directly after the current one.
     */
    fun insertionIndexForPlayNext(currentIndex: Int, size: Int): Int =
        if (size <= 0) 0 else (currentIndex + 1).coerceIn(0, size)

    private fun buildPlan(from: Int, to: Int, currentIndex: Int): MovePlan {
        val newCurrent = when (currentIndex) {
            from -> to
            to -> from
            else -> currentIndex
        }
        return MovePlan(from = from, to = to, newCurrentIndex = newCurrent)
    }
}
