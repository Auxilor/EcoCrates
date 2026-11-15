package com.willfp.ecocrates.reward

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecocrates.plugin

/**
 * How a roll with no multi-reward animation shows several rewards.
 */
enum class MultiRewardFallback {
    /** The roll plays once per reward, one after another. */
    SEQUENTIAL,

    /** The roll plays for the first reward, then the bonus rewards are given straight after. */
    PRIMARY;

    companion object {
        /**
         * Find a fallback by name, ignoring case.
         *
         * @param value The name to look up.
         * @return The fallback, or null if [value] is null or unknown.
         */
        fun parse(value: String?): MultiRewardFallback? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

/**
 * The multi-reward options for one reward source.
 *
 * @property fallback How rolls with no multi-reward animation show several rewards.
 * @property isBonusRerollable If bonus rewards can be rerolled at all.
 * @property bonusChanceMode How the bonus chances combine.
 */
data class MultiRewardSettings(
    val fallback: MultiRewardFallback,
    val isBonusRerollable: Boolean,
    val bonusChanceMode: BonusChanceMode
) {
    companion object {
        private const val SECTION = "multi-reward"

        /**
         * Resolve the settings key by key: the local value wins, then the global one, then the default.
         * A value that cannot be read falls through to the next layer.
         *
         * @param local Reads a key from the source's own settings.
         * @param global Reads a key from the plugin-wide settings.
         * @return The resolved settings.
         */
        fun resolve(local: (String) -> String?, global: (String) -> String?): MultiRewardSettings {
            fun <T : Any> layered(key: String, parse: (String?) -> T?, default: T): T =
                parse(local(key)) ?: parse(global(key)) ?: default

            return MultiRewardSettings(
                fallback = layered("fallback", MultiRewardFallback::parse, MultiRewardFallback.SEQUENTIAL),
                isBonusRerollable = layered("bonus-rerollable", ::parseBoolean, true),
                bonusChanceMode = layered("bonus-chance-mode", BonusChanceMode::parse, BonusChanceMode.OVERALL)
            )
        }

        /**
         * Resolve the settings for a source config against the plugin config.
         *
         * @param config The source's config, holding an optional `multi-reward` section,
         * or null to use only config.yml.
         * @return The resolved settings.
         */
        fun fromConfig(config: Config?): MultiRewardSettings = resolve(
            local = { key -> config?.get("$SECTION.$key")?.toString() },
            global = { key -> plugin.configYml["$SECTION.$key"]?.toString() }
        )

        private fun parseBoolean(value: String?): Boolean? = value?.run { lowercase().toBooleanStrictOrNull() }
    }
}
