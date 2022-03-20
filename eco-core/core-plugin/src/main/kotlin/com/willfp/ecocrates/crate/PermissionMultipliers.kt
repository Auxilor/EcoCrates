package com.willfp.ecocrates.crate

import com.willfp.eco.core.config.updating.ConfigUpdater
import com.willfp.ecocrates.EcoCratesPlugin
import com.willfp.ecocrates.crate.placed.PlacedCrates
import org.bukkit.entity.Player

@Suppress("UNUSED")
object PermissionMultipliers {
    private val REGISTRY = mutableListOf<PermissionMultiplier>()
    private val NO_MULTIPLIER = PermissionMultiplier("none", 1.0, 0)

    /**
     * Get the permission multiplier for a given player.
     *
     * @param player The player.
     * @return The multiplier.
     */
    @JvmStatic
    fun getForPlayer(player: Player): PermissionMultiplier {
        var current = NO_MULTIPLIER

        for (multiplier in REGISTRY) {
            if (multiplier.priority < current.priority) {
                continue
            }

            if (!player.hasPermission(multiplier.permission)) {
                continue
            }

            current = multiplier
        }

        return current
    }

    /**
     * List of all registered crates.
     *
     * @return The crates.
     */
    @JvmStatic
    fun values(): List<PermissionMultiplier> {
        return REGISTRY.toList()
    }

    @ConfigUpdater
    @JvmStatic
    fun update(plugin: EcoCratesPlugin) {
        REGISTRY.clear()

        for (config in plugin.configYml.getSubsections("permission-multipliers")) {
            val multiplier = PermissionMultiplier(
                config.getString("permission"),
                config.getDouble("multiplier"),
                config.getInt("priority")
            )
            REGISTRY.add(multiplier)
        }
    }
}
