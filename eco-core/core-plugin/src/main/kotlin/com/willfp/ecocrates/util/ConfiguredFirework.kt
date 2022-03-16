package com.willfp.ecocrates.util;

import com.willfp.eco.core.config.interfaces.Config
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Firework

data class ConfiguredFirework(
    private val power: Int,
    private val type: FireworkEffect.Type,
    private val colors: Collection<Color>,
    private val fadeColors: Collection<Color>,
    private val trail: Boolean,
    private val flicker: Boolean
) {
    fun launch(location: Location) {
        val world = location.world ?: return
        val firework = world.spawnEntity(location, EntityType.FIREWORK) as Firework
        val meta = firework.fireworkMeta
        meta.clearEffects()
        meta.power = power
        meta.addEffect(
            FireworkEffect.builder()
                .trail(trail)
                .flicker(flicker)
                .withColor(colors)
                .withFade(fadeColors)
                .with(type)
                .build()
        )
        firework.fireworkMeta = meta
    }

    companion object {
        fun fromConfig(config: Config): ConfiguredFirework {
            return ConfiguredFirework(
                config.getInt("power"),
                FireworkEffect.Type.valueOf(config.getString("type").uppercase()),
                config.getStrings("colors").map { Color.fromRGB(it.toInt(16)) },
                config.getStrings("fadeColors").map { Color.fromRGB(it.toInt(16)) },
                config.getBool("trail"),
                config.getBool("flicker")
            )
        }
    }
}
