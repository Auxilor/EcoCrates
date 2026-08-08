package com.willfp.ecocrates.envoy

/**
 * Fills the envoy placeholders in a lang.yml message.
 *
 * These are deliberately the same names the rarity's open-effects chain
 * injects as NamedValues, so `%envoy_rarity%` means the same thing
 * in lang.yml as it does in an effect config.
 *
 * Placeholders whose source is null are left untouched rather than blanked,
 * so a message using %envoy_rarity% in a category-only context is obvious
 * in game instead of silently becoming an empty string.
 */
internal fun String.withEnvoyPlaceholders(
    category: EnvoyCategory? = null,
    rarity: EnvoyRarity? = null
): String {
    var result = this

    if (category != null) {
        result = result.replace("%envoy_category%", category.name)
    }

    if (rarity != null) {
        result = result.replace("%envoy_rarity%", rarity.displayName)

        // A rarity always knows its category, so this fills in even when the
        // caller only had a rarity to hand.
        if (category == null) {
            Envoys[rarity.categoryId]?.let {
                result = result.replace("%envoy_category%", it.name)
            }
        }
    }

    return result
}
