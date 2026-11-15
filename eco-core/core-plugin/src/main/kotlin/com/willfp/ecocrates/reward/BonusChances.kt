package com.willfp.ecocrates.reward

import com.willfp.eco.core.placeholder.context.placeholderContext
import com.willfp.eco.util.evaluateExpression
import com.willfp.ecocrates.plugin
import org.bukkit.entity.Player
import java.util.concurrent.ThreadLocalRandom

/**
 * How a list of bonus chances is rolled into a number of bonus rewards.
 */
enum class BonusChanceMode {
    /** One roll, every chance above it is a bonus reward. */
    OVERALL,

    /** Chances are rolled in order, stopping at the first miss. */
    CHAINED,

    /** Every chance is rolled on its own. */
    INDEPENDENT;

    companion object {
        /**
         * Find a mode by name, ignoring case.
         *
         * @param value The name to look up.
         * @return The mode, or null if [value] is null or unknown.
         */
        fun parse(value: String?): BonusChanceMode? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

/**
 * Works out how many bonus rewards a list of chances gives.
 */
object BonusCount {
    private const val MAX_CHANCE = 100.0

    /**
     * Count the bonus rewards won.
     *
     * @param chances Percentage chances, clamped to 0..100.
     * @param mode How the chances are rolled.
     * @param roll Returns a value in [0, 100). A hit is a roll below the chance.
     * @return The number of bonus rewards.
     */
    fun count(chances: List<Double>, mode: BonusChanceMode, roll: () -> Double): Int {
        val clamped = chances.map { it.coerceIn(minimumValue = 0.0, maximumValue = MAX_CHANCE) }

        if (clamped.isEmpty()) {
            return 0
        }

        return when (mode) {
            BonusChanceMode.OVERALL -> {
                val rolled = roll()
                clamped.count { it > rolled }
            }

            BonusChanceMode.CHAINED -> {
                clamped.takeWhile { roll() < it }.size
            }

            BonusChanceMode.INDEPENDENT -> {
                clamped.count { roll() < it }
            }
        }
    }
}

/**
 * Bonus chance expressions, evaluated per player.
 *
 * @param expressions The chance expressions, as percentages.
 */
class BonusChances(private val expressions: List<String>) {
    @Suppress("U003")
    val size: Int
        get() = expressions.size

    val isEmpty: Boolean
        get() = expressions.isEmpty()

    /**
     * Evaluate every expression for a player.
     *
     * @param player The player.
     * @return The chances, with non-finite results as 0.
     */
    fun evaluate(player: Player): List<Double> = expressions.map { expression ->
        evaluateExpression(expression, placeholderContext(player = player)).takeIf { it.isFinite() } ?: 0.0
    }

    /**
     * Roll the bonus reward count for a player.
     *
     * @param player The player.
     * @param mode How the chances are rolled.
     * @return The number of bonus rewards.
     */
    fun count(player: Player, mode: BonusChanceMode): Int =
        BonusCount.count(evaluate(player), mode) { ThreadLocalRandom.current().nextDouble(MAX_ROLL) }

    /**
     * Log a warning if the chances are plain numbers that are not in descending order,
     * as the overall mode sorts them.
     *
     * @param owner The source the chances belong to, as it starts the warning.
     * @param mode How the chances are rolled.
     */
    fun warnIfUnsorted(owner: String, mode: BonusChanceMode) {
        val values = expressions.map { it.trim().toDoubleOrNull() }

        if (
            mode == BonusChanceMode.OVERALL
            && values.all { it != null }
            && !isDescending(values.filterNotNull())
        ) {
            plugin.logger.warning(
                "$owner has bonus-chances that are not in descending order; they are sorted for the overall mode."
            )
        }
    }

    companion object {
        private const val MAX_ROLL = 100.0

        /**
         * Check that values never increase.
         *
         * @param values The values.
         * @return If each value is at most the one before it.
         */
        fun isDescending(values: List<Double>): Boolean =
            values.zipWithNext().all { (first, second) -> first >= second }
    }
}
