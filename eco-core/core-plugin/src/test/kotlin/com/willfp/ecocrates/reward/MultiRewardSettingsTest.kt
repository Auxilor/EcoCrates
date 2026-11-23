package com.willfp.ecocrates.reward

import com.willfp.ecocrates.reward.BonusChanceMode.CHAINED
import com.willfp.ecocrates.reward.BonusChanceMode.INDEPENDENT
import com.willfp.ecocrates.reward.BonusChanceMode.OVERALL
import com.willfp.ecocrates.reward.MultiRewardFallback.PRIMARY
import com.willfp.ecocrates.reward.MultiRewardFallback.SEQUENTIAL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MultiRewardSettingsTest {
    private fun layer(vararg pairs: Pair<String, String>): (String) -> String? = mapOf(*pairs)::get

    @Test
    fun `defaults when empty`() = assertEquals(
        expected = MultiRewardSettings(SEQUENTIAL, true, OVERALL),
        actual = MultiRewardSettings.resolve(local = layer(), global = layer())
    )

    @Test
    fun `global overrides default`() = assertEquals(
        expected = PRIMARY,
        actual = MultiRewardSettings.resolve(local = layer(), global = layer("fallback" to "primary")).fallback
    )

    @Test
    fun `local wins per key`() {
        val settings = MultiRewardSettings.resolve(
            local = layer("bonus-rerollable" to "false"),
            global = layer("bonus-rerollable" to "true", "bonus-chance-mode" to "chained")
        )
        assertFalse(settings.isBonusRerollable)
        assertEquals(expected = CHAINED, actual = settings.bonusChanceMode)
    }

    @Test
    fun `bad value falls through`() = assertEquals(
        expected = INDEPENDENT,
        actual = MultiRewardSettings.resolve(
            local = layer("bonus-chance-mode" to "x"),
            global = layer("bonus-chance-mode" to "independent")
        ).bonusChanceMode
    )
}
