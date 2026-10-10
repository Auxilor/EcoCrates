package com.willfp.ecocrates.envoy.spawn

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpawnLocatorTest {
    @Test
    fun `shell offsets run nearest first`() {
        for (radius in 1..4) {
            val distances = SpawnLocator.shellOffsets(radius).map { (x, y, z) -> x * x + y * y + z * z }
            assertEquals(distances.sorted(), distances, "radius $radius")
        }
    }

    @Test
    fun `shell offsets cover exactly the shell surface`() {
        for (radius in 1..4) {
            val offsets = SpawnLocator.shellOffsets(radius)
            val side = 2 * radius + 1
            val inner = 2 * radius - 1

            assertEquals(side * side * side - inner * inner * inner, offsets.toSet().size)
            assertTrue(offsets.all { (x, y, z) -> maxOf(abs(x), abs(y), abs(z)) == radius })
        }
    }
}
