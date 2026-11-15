package com.willfp.ecocrates.crate.roll

import kotlin.test.Test
import kotlin.test.assertEquals

class GuiRollLayoutTest {
    @Test
    fun `one lane keeps three rows`() {
        assertEquals(expected = 3, actual = GuiRollLayout.csgoRows(lanes = 1))
    }

    @Test
    fun `four lanes use six rows`() {
        assertEquals(expected = 6, actual = GuiRollLayout.csgoRows(lanes = 4))
    }

    @Test
    fun `lanes sit between selectors`() {
        assertEquals(expected = listOf(2, 3, 4, 5), actual = (0 until 4).map { GuiRollLayout.csgoLaneRow(it) })
    }

    @Test
    fun `one lane mask unchanged`() {
        assertEquals(
            expected = listOf("111121111", "000000000", "111121111"),
            actual = List(3) { GuiRollLayout.csgoMaskRow(row = it, lanes = 1) }
        )
    }

    @Test
    fun `mask has a row per lane`() {
        assertEquals(
            expected = listOf("111121111", "000000000", "000000000", "111121111"),
            actual = List(4) { GuiRollLayout.csgoMaskRow(row = it, lanes = 2) }
        )
    }

    @Test
    fun `one lane stops on scrolls`() {
        assertEquals(expected = 35, actual = GuiRollLayout.csgoLaneStop(lane = 0, lanes = 1, scrolls = 35))
    }

    @Test
    fun `lanes stop in turn`() {
        assertEquals(
            expected = listOf(34, 40, 46, 53),
            actual = (0 until 4).map { GuiRollLayout.csgoLaneStop(lane = it, lanes = 4, scrolls = 35) }
        )
    }

    @Test
    fun `one payline is middle row`() {
        assertEquals(expected = listOf(3), actual = GuiRollLayout.paylineRows(count = 1))
    }

    @Test
    fun `paylines fill reel rows`() {
        assertEquals(
            expected = listOf(2, 3, 4),
            actual = GuiRollLayout.paylineRows(count = GuiRollLayout.slotMachineCapacity).sorted()
        )
    }

    @Test
    fun `elimination keeps one loser`() {
        assertEquals(expected = 6, actual = GuiRollLayout.eliminationCapacity(configured = 7))
    }

    @Test
    fun `elimination capacity clamped`() {
        assertEquals(
            expected = listOf(1, 6),
            actual = listOf(1, 20).map { GuiRollLayout.eliminationCapacity(configured = it) }
        )
    }

    @Test
    fun `one reward candidates same`() {
        assertEquals(
            expected = listOf(2, 5, 7),
            actual = listOf(0, 5, 9).map { GuiRollLayout.eliminationCandidates(configured = it, rewards = 1) }
        )
    }

    @Test
    fun `candidates fit every reward`() {
        assertEquals(expected = 5, actual = GuiRollLayout.eliminationCandidates(configured = 3, rewards = 4))
    }
}
