package com.willfp.ecocrates.reward

import org.bukkit.entity.Player

/**
 * Replaces `%max_rewards%` and `%bonus_chance_<n>%` in text.
 */
object BonusPlaceholders {
    private const val MAX_REWARDS = "%max_rewards%"
    private const val BONUS_CHANCE_PREFIX = "%bonus_chance_"
    private val BONUS_CHANCE = Regex("%bonus_chance_(\\d+)%")

    /**
     * Read the chance at a 1-based position.
     *
     * @param chances The bonus chances.
     * @param position The 1-based position.
     * @return The chance, or 0 for a position past the end.
     */
    fun chanceAt(chances: List<Double>, position: Int): Double =
        chances.getOrElse(position - 1) { 0.0 }

    /**
     * Replace the placeholders in text.
     *
     * @param text The text.
     * @param maxRewards The value of `%max_rewards%`.
     * @param chances Supplies the evaluated chances, called only if the text needs them.
     * @param format Formats a chance.
     * @return The text with the placeholders replaced.
     */
    fun replace(text: String, maxRewards: Int, chances: () -> List<Double>, format: (Double) -> String): String {
        val withMax = replaceMaxRewards(text, maxRewards)

        if (!BONUS_CHANCE.containsMatchIn(withMax)) {
            return withMax
        }

        val evaluated = chances()

        return BONUS_CHANCE.replace(withMax) { match ->
            format(chanceAt(evaluated, match.groupValues[1].toIntOrNull() ?: 0))
        }
    }

    /**
     * Replace the placeholders in text for a player.
     *
     * @param text The text.
     * @param source The source whose bonus chances are used.
     * @param player The player the chances are evaluated for.
     * @return The text with the placeholders replaced.
     */
    fun replace(text: String, source: RewardSource, player: Player): String = replace(
        text,
        maxRewards = maxRewards(source),
        chances = { source.bonusChances.evaluate(player) },
        format = Reward::formatChance
    )

    /**
     * Replace only `%max_rewards%`, for text with no player.
     *
     * @param text The text.
     * @param source The source whose bonus chances are used.
     * @return The text with `%max_rewards%` replaced.
     */
    fun replaceMaxRewards(text: String, source: RewardSource): String =
        replaceMaxRewards(text, maxRewards(source))

    /**
     * Check if text holds a placeholder this object replaces.
     *
     * @param text The text.
     * @return If the text holds `%max_rewards%` or `%bonus_chance_`.
     */
    fun hasPlaceholder(text: String): Boolean =
        text.contains(MAX_REWARDS) || text.contains(BONUS_CHANCE_PREFIX)

    private fun maxRewards(source: RewardSource): Int = 1 + source.bonusChances.size

    private fun replaceMaxRewards(text: String, maxRewards: Int): String =
        text.replace(oldValue = MAX_REWARDS, newValue = maxRewards.toString())
}
