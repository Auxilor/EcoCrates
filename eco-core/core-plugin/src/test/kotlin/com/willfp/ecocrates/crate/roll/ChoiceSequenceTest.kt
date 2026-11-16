package com.willfp.ecocrates.crate.roll

import com.willfp.ecocrates.reward.PickRule
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChoiceSequenceTest {
    private val rule = PickRule<String>(
        maxPerOpen = { if (it == "jackpot") 1 else -1 },
        isBonusEligible = { it != "grand" },
        maxWinsLeft = { Int.MAX_VALUE }
    )

    private val all = listOf("jackpot", "grand", "coins")

    private fun sequence(
        planned: List<String>,
        candidates: List<String> = all,
        heldElsewhere: List<String> = emptyList(),
        firstIsBonus: Boolean = false
    ) = ChoiceSequence(
        planned = planned,
        isBonus = { it > 0 || firstIsBonus },
        candidates = candidates,
        heldElsewhere = heldElsewhere,
        rule = rule
    )

    @Test
    fun `main choice shows all`() {
        assertEquals(expected = all, actual = sequence(planned = listOf("coins")).shown)
    }

    @Test
    fun `main reroll keeps max per open`() {
        val choices = sequence(planned = listOf("coins"), heldElsewhere = listOf("jackpot"))
        assertEquals(expected = listOf("grand", "coins"), actual = choices.shown)
    }

    @Test
    fun `main falls back if all capped`() {
        val choices = sequence(
            planned = listOf("jackpot"),
            candidates = listOf("jackpot"),
            heldElsewhere = listOf("jackpot")
        )
        assertEquals(expected = listOf("jackpot"), actual = choices.shown)
    }

    @Test
    fun `bonus hides ineligible`() {
        val choices = sequence(planned = listOf("coins", "coins"))
        choices.choose("coins")
        assertEquals(expected = listOf("jackpot", "coins"), actual = choices.shown)
    }

    @Test
    fun `bonus hides capped choice`() {
        val choices = sequence(planned = listOf("coins", "jackpot"))
        choices.choose("jackpot")
        assertEquals(expected = listOf("coins"), actual = choices.shown)
    }

    @Test
    fun `bonus counts held elsewhere`() {
        val choices = sequence(planned = listOf("coins"), heldElsewhere = listOf("jackpot"), firstIsBonus = true)
        assertEquals(expected = listOf("coins"), actual = choices.shown)
    }

    @Test
    fun `none left drops the bonus`() {
        val choices = sequence(planned = listOf("grand", "jackpot", "jackpot"), candidates = listOf("jackpot", "grand"))
        choices.choose("jackpot")
        assertTrue(choices.isComplete())
        assertEquals(expected = setOf(1, 2), actual = choices.droppedSlots)
    }

    @Test
    fun `nothing at start drops`() {
        val choices = sequence(
            planned = listOf("jackpot"),
            candidates = listOf("jackpot"),
            heldElsewhere = listOf("jackpot"),
            firstIsBonus = true
        )
        assertTrue(choices.isComplete())
        assertEquals(expected = setOf(0), actual = choices.droppedSlots)
    }

    @Test
    fun `auto pick keeps max per open`() {
        repeat(times = 20) { seed ->
            val choices = sequence(planned = listOf("coins", "jackpot", "jackpot"))
            choices.choose("jackpot")
            choices.completeAtRandom(Random(seed))
            assertEquals(expected = listOf("jackpot", "coins", "coins"), actual = choices.choices)
        }
    }

    @Test
    fun `auto pick fills every choice`() {
        val choices = sequence(planned = listOf("coins", "coins", "coins"))
        choices.completeAtRandom(Random(seed = 1))
        assertTrue(choices.isComplete())
        assertTrue(choices.choices.count { it == "jackpot" } <= 1)
        assertTrue(choices.choices.drop(1).none { it == "grand" })
    }

    @Test
    fun `dropped slot is not held`() {
        val choices = sequence(
            planned = listOf("grand", "jackpot", "coins"),
            candidates = listOf("jackpot", "grand", "coins")
        )
        choices.choose("grand")
        choices.choose("jackpot")
        assertEquals(expected = listOf("coins"), actual = choices.shown)
    }
}
