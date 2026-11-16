package com.willfp.ecocrates.crate.roll

import kotlin.test.Test
import kotlin.test.assertEquals

class RewardStaggerTest {
    @Test
    fun `first reward locks first`() {
        assertEquals(expected = 80, actual = RewardStagger.lockTick(index = 0, firstLock = 80, gap = 20))
    }

    @Test
    fun `rewards lock gap apart`() {
        assertEquals(
            expected = listOf(80, 100, 120),
            actual = (0 until 3).map { RewardStagger.lockTick(index = it, firstLock = 80, gap = 20) }
        )
    }

    @Test
    fun `one reward end is unchanged`() {
        assertEquals(expected = 100, actual = RewardStagger.endTick(count = 1, firstLock = 80, gap = 20, hold = 20))
    }

    @Test
    fun `end holds after last lock`() {
        assertEquals(expected = 80, actual = RewardStagger.endTick(count = 3, firstLock = 45, gap = 5, hold = 25))
    }

    @Test
    fun `nothing locked before first`() {
        assertEquals(expected = 0, actual = RewardStagger.lockedCount(tick = 79, firstLock = 80, gap = 20, count = 3))
    }

    @Test
    fun `locked count grows in turn`() {
        assertEquals(
            expected = listOf(1, 1, 2, 3, 3),
            actual = listOf(80, 99, 100, 120, 500).map {
                RewardStagger.lockedCount(tick = it, firstLock = 80, gap = 20, count = 3)
            }
        )
    }

    @Test
    fun `zero gap locks all at once`() {
        assertEquals(expected = 4, actual = RewardStagger.lockedCount(tick = 0, firstLock = 0, gap = 0, count = 4))
    }
}
