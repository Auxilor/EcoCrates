package com.willfp.ecocrates.crate.roll

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RollValidationTest {
    private val rolls = mapOf("csgo" to true, "pick" to true, "flash" to false)

    @Test
    fun `gui roll is accepted`() {
        val result = RollValidation.resolveGuiRoll("pick") { rolls[it] }
        assertEquals("pick", result.id)
        assertNull(result.warning)
    }

    @Test
    fun `non-gui roll falls back to csgo with warning`() {
        val result = RollValidation.resolveGuiRoll("flash") { rolls[it] }
        assertEquals("csgo", result.id)
        assertNotNull(result.warning)
    }

    @Test
    fun `unknown roll falls back to csgo for gui-only sources`() {
        val result = RollValidation.resolveGuiRoll("nope") { rolls[it] }
        assertEquals("csgo", result.id)
        assertNotNull(result.warning)
    }
}
