package com.willfp.ecocrates.crate.roll

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class RingItemsTest {
    @Test
    fun `capacity leaves one filler`() {
        assertEquals(expected = 11, actual = RingItems.capacity(configured = 12))
    }

    @Test
    fun `capacity is at least one`() {
        assertEquals(expected = 1, actual = RingItems.capacity(configured = 1))
    }

    @Test
    fun `one reward count unchanged`() {
        assertEquals(
            expected = listOf(1, 2, 8),
            actual = listOf(1 to 1, 1 to 2, 8 to 2).map { (configured, minimum) ->
                RingItems.count(configured = configured, minimum = minimum, rewards = 1)
            }
        )
    }

    @Test
    fun `count fits every reward`() {
        assertEquals(expected = 6, actual = RingItems.count(configured = 3, minimum = 1, rewards = 5))
    }

    @Test
    fun `count keeps larger config`() {
        assertEquals(expected = 12, actual = RingItems.count(configured = 12, minimum = 1, rewards = 5))
    }

    @Test
    fun `order holds each reward once`() {
        assertEquals(
            expected = listOf(RingItems.FILLER, RingItems.FILLER, RingItems.FILLER, 0, 1, 2),
            actual = RingItems.order(itemCount = 6, rewards = 3, random = Random(seed = 7)).sorted()
        )
    }

    @Test
    fun `first stop uses steps`() {
        assertEquals(expected = listOf(32), actual = RingItems.stopSteps(count = 1, steps = 32, itemCount = 8))
    }

    @Test
    fun `later stops lap and spread`() {
        assertEquals(expected = listOf(32, 10, 10), actual = RingItems.stopSteps(count = 3, steps = 32, itemCount = 8))
    }

    @Test
    fun `stop indices follow cursor`() {
        assertEquals(
            expected = listOf(0, 2, 4),
            actual = RingItems.stopIndices(stopSteps = listOf(32, 10, 10), itemCount = 8)
        )
    }

    @Test
    fun `stops never share an item`() {
        (2..12).forEach { itemCount ->
            (1 until itemCount).forEach { count ->
                val indices = RingItems.stopIndices(
                    stopSteps = RingItems.stopSteps(count = count, steps = 32, itemCount = itemCount),
                    itemCount = itemCount
                )
                assertEquals(expected = count, actual = indices.distinct().size)
            }
        }
    }

    @Test
    fun `winners in reward order`() {
        assertEquals(
            expected = listOf("a", "b", "c"),
            actual = RingItems.winners(
                listOf(2 to "c", RingItems.FILLER to "x", 0 to "a", RingItems.FILLER to "y", 1 to "b")
            )
        )
    }
}
