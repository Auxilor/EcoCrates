package com.willfp.ecocrates.crate.reroll

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.placeholder.StaticPlaceholder
import com.willfp.eco.core.price.ConfiguredPrice

/**
 * Unified reroll configuration for a crate, resolving both the legacy
 * `can-reroll: true` option and the new `rerolls:` block into one model.
 */
class RerollProfile(
    val enabled: Boolean,
    val maxRerolls: Int,
    private val priceConfig: Config?
) {
    /**
     * Builds the price for a given reroll number, injecting `%reroll%`
     * (1-based) so the price can scale, e.g. `"%reroll%*2"`.
     * Legacy crates have no price config and are free.
     */
    fun priceFor(rerollNumber: Int): ConfiguredPrice {
        val cfg = priceConfig ?: return ConfiguredPrice.FREE
        // Clone so simultaneous rerolls by different players don't clobber
        // each other's injected %reroll% value on the shared crate config.
        val injected = cfg.clone()
        // eco's StaticPlaceholder wraps the identifier in %...%, so pass the bare name.
        injected.injectPlaceholders(StaticPlaceholder("reroll") { rerollNumber.toString() })
        return ConfiguredPrice.create(injected) ?: ConfiguredPrice.FREE
    }

    companion object {
        fun fromCrateConfig(config: Config, onDeprecated: () -> Unit): RerollProfile {
            if (config.has("rerolls")) {
                val rerolls = config.getSubsection("rerolls")
                return RerollProfile(
                    enabled = rerolls.getBool("enabled"),
                    maxRerolls = rerolls.getInt("max-rerolls"),
                    priceConfig = rerolls.getSubsectionOrNull("price")
                )
            }

            // Legacy path: one free reroll, deprecated.
            if (config.getBool("can-reroll")) {
                onDeprecated()
                return RerollProfile(enabled = true, maxRerolls = 1, priceConfig = null)
            }

            return RerollProfile(enabled = false, maxRerolls = 0, priceConfig = null)
        }
    }
}
