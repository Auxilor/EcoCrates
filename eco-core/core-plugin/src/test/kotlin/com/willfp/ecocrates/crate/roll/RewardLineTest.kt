package com.willfp.ecocrates.crate.roll

import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals

class RewardLineTest {
    private fun assertNear(expected: Double, actual: Double) {
        assertEquals(expected = expected, actual = actual, absoluteTolerance = 1.0E-9)
    }

    @Test
    fun `one reward sits on crate`() {
        val offsets = RewardLine.offsets(count = 1, yaw = 37.0)
        assertEquals(expected = 1, actual = offsets.size)
        assertNear(expected = 0.0, actual = offsets[0].x)
        assertNear(expected = 0.0, actual = offsets[0].z)
    }

    @Test
    fun `facing south runs east to west`() {
        val offsets = RewardLine.offsets(count = 3, yaw = 0.0)
        listOf(1.25, 0.0, -1.25).zip(offsets).forEach { (expected, offset) ->
            assertNear(expected = expected, actual = offset.x)
        }
        offsets.forEach { assertNear(expected = 0.0, actual = it.z) }
    }

    @Test
    fun `facing east runs north south`() {
        val offsets = RewardLine.offsets(count = 2, yaw = -90.0)
        assertNear(expected = -0.625, actual = offsets[0].z)
        assertNear(expected = 0.625, actual = offsets[1].z)
        offsets.forEach { assertNear(expected = 0.0, actual = it.x) }
    }

    @Test
    fun `line is centred on crate`() {
        val offsets = RewardLine.offsets(count = 5, yaw = 123.0)
        assertNear(expected = 0.0, actual = offsets.sumOf { it.x })
        assertNear(expected = 0.0, actual = offsets.sumOf { it.z })
    }

    @Test
    fun `neighbours are spacing apart`() {
        RewardLine.offsets(count = 4, yaw = 200.0, spacing = 1.5).zipWithNext().forEach { (first, second) ->
            assertNear(expected = 1.5, actual = hypot(x = second.x - first.x, y = second.z - first.z))
        }
    }
}
