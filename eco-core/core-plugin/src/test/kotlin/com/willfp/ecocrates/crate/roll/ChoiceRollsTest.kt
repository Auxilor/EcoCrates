package com.willfp.ecocrates.crate.roll

import kotlin.test.Test
import kotlin.test.assertEquals

class ChoiceRollsTest {
    @Test
    fun `one reward keeps boxes`() {
        assertEquals(
            expected = listOf(2, 3, 7),
            actual = listOf(1, 3, 9).map { ChoiceRolls.pickBoxes(configured = it, rewards = 1) }
        )
    }

    @Test
    fun `boxes grow past rewards`() {
        assertEquals(expected = 5, actual = ChoiceRolls.pickBoxes(configured = 3, rewards = 4))
    }

    @Test
    fun `boxes capped at seven`() {
        assertEquals(expected = 7, actual = ChoiceRolls.pickBoxes(configured = 3, rewards = 6))
    }

    @Test
    fun `to match kept in range`() {
        assertEquals(expected = listOf(2, 3, 21), actual = listOf(1, 3, 30).map { ChoiceRolls.matchToMatch(it) })
    }

    @Test
    fun `match capacity fits card`() {
        assertEquals(expected = listOf(14, 9, 1), actual = listOf(2, 3, 21).map { ChoiceRolls.matchCapacity(it) })
    }

    @Test
    fun `one set keeps losers`() {
        assertEquals(expected = 19, actual = ChoiceRolls.matchLoserCapacity(toMatch = 2, rewards = 1))
    }

    @Test
    fun `few sets keep losers`() {
        assertEquals(expected = 19, actual = ChoiceRolls.matchLoserCapacity(toMatch = 2, rewards = 4))
    }

    @Test
    fun `many sets shrink losers`() {
        assertEquals(expected = 4, actual = ChoiceRolls.matchLoserCapacity(toMatch = 2, rewards = 12))
    }

    @Test
    fun `full card has no losers`() {
        assertEquals(expected = 0, actual = ChoiceRolls.matchLoserCapacity(toMatch = 3, rewards = 10))
    }

    @Test
    fun `set progress wraps`() {
        assertEquals(
            expected = listOf(1, 2, 1, 2),
            actual = (1..4).map { ChoiceRolls.setProgress(found = it, toMatch = 2) }
        )
    }
}
