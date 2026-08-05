package com.willfp.ecocrates.envoy

import com.willfp.eco.core.config.interfaces.Config
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Location
import org.bukkit.entity.Firework

class EnvoyFireworks(
    val enabled: Boolean,
    val colors: List<Color>
) {
    fun spawn(location: Location) {
        if (!enabled) {
            return
        }

        val world = location.world ?: return

        val firework = world.spawn(location, Firework::class.java)

        firework.fireworkMeta = firework.fireworkMeta.apply {
            addEffect(
                FireworkEffect.builder()
                    .with(FireworkEffect.Type.BALL)
                    .withColor(colors.ifEmpty { listOf(Color.WHITE) })
                    .build()
            )
            power = 1
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

        fun fromConfig(config: Config?): EnvoyFireworks {
            if (config == null) {
                return EnvoyFireworks(false, emptyList())
            }

            return EnvoyFireworks(
                config.getBool("enabled"),
                config.getStrings("colors").mapNotNull { parseColorOrNull(it) }
            )
        }
    }
}
