package com.willfp.ecocrates.crate

import com.willfp.ecocrates.plugin
import org.bukkit.entity.Player

object PermissionMultipliers {
    @Volatile
    private var registry = emptyList<PermissionMultiplier>()

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

        for (multiplier in registry) {
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
    fun values(): List<PermissionMultiplier> = registry.toList()

    @JvmStatic
    fun update() {
        registry = plugin.configYml.getSubsections("permission-multipliers").map { config ->
            PermissionMultiplier(
                config.getString("permission"),
                config.getDouble("multiplier"),
                config.getInt("priority")
            )
        }
    }
}
