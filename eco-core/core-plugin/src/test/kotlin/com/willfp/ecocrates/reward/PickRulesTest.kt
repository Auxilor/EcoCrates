package com.willfp.ecocrates.reward

import kotlin.test.Test
import kotlin.test.assertEquals

class PickRulesTest {
    private val rule = PickRule<String>(
        maxPerOpen = { if (it == "jackpot") 1 else -1 },
        isBonusEligible = { it != "grand" },
        maxWinsLeft = { if (it == "perk") 1 else Int.MAX_VALUE }
    )

    private val all = listOf("jackpot", "grand", "perk", "coins")

    private fun check(
        expected: List<String>,
        taken: List<String>,
        isBonus: Boolean,
        candidates: List<String> = all,
        pending: List<String> = taken
    ) =
        assertEquals(
            expected = expected,
            actual = PickRules.eligible(
                candidates = candidates,
                taken = taken,
                isBonus = isBonus,
                rule = rule,
                pending = pending
            )
        )

    @Test
    fun `first pick keeps all`() =
        check(expected = all, taken = emptyList(), isBonus = false)

    @Test
    fun `bonus drops ineligible`() =
        check(expected = listOf("jackpot", "perk", "coins"), taken = emptyList(), isBonus = true)

    @Test
    fun `cap per open`() =
        check(expected = listOf("perk", "coins"), taken = listOf("jackpot"), isBonus = true)

    @Test
    fun `pending counts to max wins`() =
        check(expected = listOf("jackpot", "coins"), taken = listOf("perk"), isBonus = true)

    @Test
    fun `given picks skip max wins`() =
        check(
            expected = listOf("jackpot", "perk", "coins"),
            taken = listOf("perk"),
            isBonus = true,
            pending = emptyList()
        )

    @Test
    fun `given picks keep the cap`() =
        check(expected = listOf("perk", "coins"), taken = listOf("jackpot"), isBonus = true, pending = emptyList())

    @Test
    fun `no limit allows repeats`() =
        check(expected = listOf("jackpot", "perk", "coins"), taken = listOf("coins", "coins"), isBonus = true)

    @Test
    fun `all excluded is empty`() =
        check(expected = emptyList(), taken = emptyList(), isBonus = true, candidates = listOf("grand"))
}
