package com.willfp.ecocrates.reward

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BonusPlaceholdersTest {
    private val chances = listOf(50.0, 20.0)

    private fun replace(text: String) =
        BonusPlaceholders.replace(text, maxRewards = 3, chances = { chances }, format = { "<$it>" })

    @Test
    fun `reads one based`() =
        assertEquals(expected = "<50.0> <20.0>", actual = replace("%bonus_chance_1% %bonus_chance_2%"))

    @Test
    fun `past the end reads 0`() =
        assertEquals(expected = "<0.0>", actual = replace("%bonus_chance_3%"))

    @Test
    fun `zero position reads 0`() =
        assertEquals(expected = "<0.0>", actual = replace("%bonus_chance_0%"))

    @Test
    fun `huge position reads 0`() =
        assertEquals(expected = "<0.0>", actual = replace("%bonus_chance_99999999999%"))

    @Test
    fun `replaces max rewards`() =
        assertEquals(expected = "3 rewards", actual = replace("%max_rewards% rewards"))

    @Test
    fun `skips evaluation unused`() =
        assertEquals(
            expected = "3",
            actual = BonusPlaceholders.replace("%max_rewards%", 3, { error("evaluated") }, { "" })
        )

    @Test
    fun `detects max rewards`() =
        assertTrue(BonusPlaceholders.hasPlaceholder("up to %max_rewards%"))

    @Test
    fun `detects bonus chance`() =
        assertTrue(BonusPlaceholders.hasPlaceholder("%bonus_chance_2%"))

    @Test
    fun `ignores plain text`() =
        assertFalse(BonusPlaceholders.hasPlaceholder("%chance% of %weight%"))
}
