package com.willfp.ecocrates.envoy

import com.willfp.eco.core.config.interfaces.Config
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Location
import org.bukkit.entity.Firework

/**
 * A rarity's configured firework burst, fired at a crate's spawn location.
 *
 * @param amount How many fireworks to spawn, each with one [type] effect.
 * @param colors Colours applied to every spawned firework; defaults to white if empty.
 */
class EnvoyFireworks(
    val enabled: Boolean,
    val amount: Int,
    val type: FireworkEffect.Type,
    val colors: List<Color>
) {
    /** Spawns [amount] fireworks at [location], each detonating immediately with this config's effect. */
    fun spawn(location: Location) {
        if (!enabled || amount <= 0) {
            return
        }

        val world = location.world ?: return

        repeat(amount) {
            val firework = world.spawn(location, Firework::class.java)

            firework.fireworkMeta = firework.fireworkMeta.apply {
                addEffect(
                    FireworkEffect.builder()
                        .with(type)
                        .withColor(colors.ifEmpty { listOf(Color.WHITE) })
                        .build()
                )
                power = 1
            }
        }
    }

    companion object {
        private val namedColors = mapOf(
            "white" to Color.WHITE,
            "silver" to Color.SILVER,
            "gray" to Color.GRAY,
            "black" to Color.BLACK,
            "red" to Color.RED,
            "maroon" to Color.MAROON,
            "yellow" to Color.YELLOW,
            "olive" to Color.OLIVE,
            "lime" to Color.LIME,
            "green" to Color.GREEN,
            "aqua" to Color.AQUA,
            "teal" to Color.TEAL,
            "blue" to Color.BLUE,
            "navy" to Color.NAVY,
            "fuchsia" to Color.FUCHSIA,
            "purple" to Color.PURPLE,
            "orange" to Color.ORANGE
        )

        /**
         * Parses a colour name (e.g. `lime`) or a hex code (e.g. `#a8e063`).
         * Unknown values are dropped rather than failing the whole config.
         */
        internal fun parseColorOrNull(raw: String): Color? {
            namedColors[raw.trim().lowercase()]?.let { return it }

            val hex = raw.trim().removePrefix("#")

            if (hex.length != 6) {
                return null
            }

            val rgb = hex.toIntOrNull(16) ?: return null

            return Color.fromRGB(rgb)
        }

        /**
         * Config uses friendly lowercase names (ball, large_ball, burst, star,
         * creeper). Bukkit's own enum spells the big one BALL_LARGE, so that
         * one is aliased rather than made the config's problem.
         */
        private fun parseType(raw: String): FireworkEffect.Type {
            val normalised = when (raw.trim().lowercase()) {
                "large_ball", "ball_large" -> "BALL_LARGE"
                else -> raw.trim().uppercase()
            }

            return runCatching { FireworkEffect.Type.valueOf(normalised) }
                .getOrDefault(FireworkEffect.Type.BALL)
        }

        fun fromConfig(config: Config?): EnvoyFireworks {
            if (config == null) {
                return EnvoyFireworks(false, 0, FireworkEffect.Type.BALL, emptyList())
            }

            val type = parseType(config.getString("type"))

            return EnvoyFireworks(
                config.getBool("enabled"),
                config.getInt("amount"),
                type,
                config.getStrings("colors").mapNotNull { parseColorOrNull(it) }
            )
        }
    }
}
