package com.willfp.ecocrates.util

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeightedTest {
    @Test
    fun `empty collection returns null`() {
        assertNull(emptyList<String>().weightedRandom { 1.0 })
    }

    @Test
    fun `zero total weight still returns an element`() {
        val result = listOf("a", "b").weightedRandom { 0.0 }
        assertTrue(result == "a" || result == "b")
    }

    @Test
    fun `negative weights are treated as zero`() {
        // "b" is the only element with positive weight, so it must always win.
        repeat(50) {
            assertEquals("b", listOf("a", "b").weightedRandom { if (it == "a") -5.0 else 1.0 })
        }
    }

    @Test
    fun `single positive weight always wins`() {
        repeat(50) {
            assertEquals("heavy", listOf("heavy", "zero").weightedRandom { if (it == "heavy") 10.0 else 0.0 })
        }
    }

    @Test
    fun `distribution roughly tracks weights`() {
        val random = Random(1234)
        val counts = mutableMapOf("common" to 0, "rare" to 0)

        repeat(10_000) {
            val picked = listOf("common", "rare")
                .weightedRandom(random) { if (it == "common") 9.0 else 1.0 }!!
            counts[picked] = counts.getValue(picked) + 1
        }

        // 90/10 split; allow generous slack so the test is not flaky.
        assertTrue(counts.getValue("common") in 8500..9500, "common was ${counts["common"]}")
        assertTrue(counts.getValue("rare") in 500..1500, "rare was ${counts["rare"]}")
    }
}
