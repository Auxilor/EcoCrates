package com.willfp.ecocrates.reward

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PendingRewardEntryTest {
    @Test
    fun `legacy two-part entry is a crate`() {
        assertEquals(
            PendingRewardEntry("crate", "demo", "diamond_sword"),
            PendingRewardEntry.parse("demo:diamond_sword")
        )
    }

    @Test
    fun `typed three-part entry keeps its type`() {
        assertEquals(
            PendingRewardEntry("pouch", "mining", "1000_coins"),
            PendingRewardEntry.parse("pouch:mining:1000_coins")
        )
    }

    @Test
    fun `serialize round trips`() {
        val entry = PendingRewardEntry("pouch", "mining", "bedrock")
        assertEquals(entry, PendingRewardEntry.parse(entry.serialize()))
    }

    @Test
    fun `malformed entries are rejected`() {
        assertNull(PendingRewardEntry.parse(""))
        assertNull(PendingRewardEntry.parse("justone"))
        assertNull(PendingRewardEntry.parse("a:b:c:d"))
        assertNull(PendingRewardEntry.parse(":reward"))
        assertNull(PendingRewardEntry.parse("pouch::reward"))
    }
}
