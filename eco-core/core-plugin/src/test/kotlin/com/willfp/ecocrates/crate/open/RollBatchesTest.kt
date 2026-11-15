package com.willfp.ecocrates.crate.open

import com.willfp.ecocrates.reward.MultiRewardFallback
import kotlin.test.Test
import kotlin.test.assertEquals

class RollBatchesTest {
    private val three = listOf(1, 2, 3)

    private val five = listOf(1, 2, 3, 4, 5)

    @Test
    fun `sequential one per batch`() = assertEquals(
        expected = listOf(listOf(1), listOf(2), listOf(3)),
        actual = splitIntoRollBatches(picks = three, maxRewards = 1, fallback = MultiRewardFallback.SEQUENTIAL)
    )

    @Test
    fun `primary keeps first only`() = assertEquals(
        expected = listOf(listOf(1)),
        actual = splitIntoRollBatches(picks = three, maxRewards = 1, fallback = MultiRewardFallback.PRIMARY)
    )

    @Test
    fun `native batches ignore fallback`() {
        for (fallback in MultiRewardFallback.entries) {
            assertEquals(
                expected = listOf(listOf(1, 2), listOf(3, 4), listOf(5)),
                actual = splitIntoRollBatches(picks = five, maxRewards = 2, fallback = fallback)
            )
        }
    }

    @Test
    fun `unlimited gives one batch`() = assertEquals(
        expected = listOf(five),
        actual = splitIntoRollBatches(
            picks = five,
            maxRewards = Int.MAX_VALUE,
            fallback = MultiRewardFallback.SEQUENTIAL
        )
    )

    @Test
    fun `batch holds earlier picks`() = assertEquals(
        expected = listOf(1, 2),
        actual = heldOutsideBatch(picks = five, batch = five.subList(fromIndex = 2, toIndex = 4), isReroll = false)
    )

    @Test
    fun `reroll holds other picks`() = assertEquals(
        expected = listOf(1, 2, 4, 5),
        actual = heldOutsideBatch(picks = five, batch = five.subList(fromIndex = 2, toIndex = 3), isReroll = true)
    )
}
