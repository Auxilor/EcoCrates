package com.willfp.ecocrates.envoy.spawn

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RadiusBoxTest {
    @Test
    fun `zero radius always rolls the centre`() {
        val box = RadiusBox("world", BlockPos(10, 64, -20), 0, 0, 0)
        repeat(20) {
            assertEquals(BlockPos(10, 64, -20), box.roll())
        }
    }

    @Test
    fun `rolls stay inside the box on every axis`() {
        val box = RadiusBox("world", BlockPos(0, 70, 0), 8, 3, 16)
        val random = Random(99)

        repeat(2000) {
            val pos = box.roll(random)
            assertTrue(pos.x in -8..8, "x was ${pos.x}")
            assertTrue(pos.y in 67..73, "y was ${pos.y}")
            assertTrue(pos.z in -16..16, "z was ${pos.z}")
        }
    }

    @Test
    fun `negative radii are treated as zero`() {
        val box = RadiusBox("world", BlockPos(5, 5, 5), -4, -4, -4)
        repeat(20) {
            assertEquals(BlockPos(5, 5, 5), box.roll())
        }
    }

    @Test
    fun `rolls cover the full range given enough samples`() {
        val box = RadiusBox("world", BlockPos(0, 0, 0), 2, 0, 0)
        val seen = mutableSetOf<Int>()
        val random = Random(7)

        repeat(500) { seen.add(box.roll(random).x) }

        assertEquals(setOf(-2, -1, 0, 1, 2), seen)
    }
}
