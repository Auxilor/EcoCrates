package com.willfp.ecocrates.reward

import com.willfp.ecocrates.reward.BonusChanceMode.CHAINED
import com.willfp.ecocrates.reward.BonusChanceMode.INDEPENDENT
import com.willfp.ecocrates.reward.BonusChanceMode.OVERALL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BonusCountTest {
    private fun rolls(vararg values: Double): () -> Double {
        val iterator = values.iterator()
        return { iterator.next() }
    }

    @Test
    fun `overall counts below roll`() =
        assertEquals(expected = 2, actual = BonusCount.count(listOf(50.0, 20.0, 5.0), OVERALL, rolls(10.0)))

    @Test
    fun `overall sorts unsorted`() =
        assertEquals(expected = 2, actual = BonusCount.count(listOf(5.0, 50.0, 20.0), OVERALL, rolls(10.0)))

    @Test
    fun `overall rolls once`() =
        assertEquals(expected = 3, actual = BonusCount.count(listOf(100.0, 100.0, 100.0), OVERALL, rolls(99.9)))

    @Test
    fun `chained stops at miss`() =
        assertEquals(expected = 1, actual = BonusCount.count(listOf(50.0, 20.0, 90.0), CHAINED, rolls(10.0, 30.0, 0.0)))

    @Test
    fun `independent counts hits`() =
        assertEquals(
            expected = 2,
            actual = BonusCount.count(listOf(50.0, 20.0, 90.0), INDEPENDENT, rolls(10.0, 30.0, 0.0))
        )

    @Test
    fun `zero never hits`() =
        assertEquals(expected = 0, actual = BonusCount.count(listOf(0.0), INDEPENDENT, rolls(0.0)))

    @Test
    fun `clamps out of range`() =
        assertEquals(expected = 1, actual = BonusCount.count(listOf(250.0, -40.0), INDEPENDENT, rolls(99.0, 0.0)))

    @Test
    fun `empty is zero`() =
        assertEquals(expected = 0, actual = BonusCount.count(emptyList(), OVERALL, rolls()))

    @Test
    fun `parse mode ignores case`() =
        assertEquals(expected = CHAINED, actual = BonusChanceMode.parse("Chained"))

    @Test
    fun `parse unknown is null`() =
        assertNull(BonusChanceMode.parse("sometimes"))

    @Test
    fun `descending check`() {
        assertTrue(BonusChances.isDescending(listOf(50.0, 20.0, 20.0)))
        assertFalse(BonusChances.isDescending(listOf(5.0, 50.0)))
    }
}
