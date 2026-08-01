package com.willfp.ecocrates.envoy

import com.willfp.ecocrates.envoy.compass.CompassRenderer
import com.willfp.ecocrates.envoy.compass.EnvoyCompasses
import com.willfp.ecocrates.envoy.session.EnvoySessions
import com.willfp.ecocrates.plugin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot

object CompassListener : Listener {
    @EventHandler
    fun handleUse(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) {
            return
        }

        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val player = event.player
        val held = player.inventory.itemInMainHand
        val categoryId = held.compassCategoryId ?: return
        val category = Envoys[categoryId] ?: return
        val compass = category.compass ?: return

        if (!compass.matches(held)) {
            return
        }

        event.isCancelled = true

        val session = EnvoySessions.active

        if (session == null) {
            player.sendMessage(plugin.langYml.getMessage("envoy-not-active"))
            return
        }

        if (session.category.id != categoryId) {
            player.sendMessage(
                plugin.langYml.getMessage("compass-wrong-envoy")
                    .withEnvoyPlaceholders(session.category)
            )
            return
        }

        if (EnvoyCompasses.isActive(player)) {
            player.sendMessage(plugin.langYml.getMessage("compass-already-active"))
            return
        }

        val cooldown = EnvoyCooldowns.remainingFlare(player, "compass:$categoryId")

        if (cooldown > 0) {
            player.sendMessage(
                plugin.langYml.getMessage("compass-on-cooldown")
                    .replace("%seconds%", Math.ceil(cooldown / 20.0).toInt().toString())
            )
            return
        }

        // Refuse rather than burn the item on a blank locator bar. Uses the
        // exact same predicate as CompassRenderer so this can never approve
        // activation for a session the renderer would show nothing for.
        val hasAnythingVisible = session.spawns.any {
            CompassRenderer.isVisibleFor(it, player, compass)
        }

        if (!hasAnythingVisible) {
            player.sendMessage(plugin.langYml.getMessage("compass-nothing-to-track"))
            return
        }

        if (!compass.price.canAfford(player)) {
            player.sendMessage(
                plugin.langYml.getMessage("compass-cannot-afford")
                    .replace("%price%", compass.price.getDisplay(player))
            )
            return
        }

        if (!EnvoyCompasses.activate(player, category)) {
            return
        }

        compass.price.pay(player)
        consumeOne(player)
        EnvoyCooldowns.applyFlare(player, "compass:$categoryId", compass.cooldownTicks)

        player.sendMessage(
            plugin.langYml.getMessage("compass-activated")
                .replace("%seconds%", (compass.durationTicks / 20).toString())
        )
    }

    /** Compass time is never banked across a relog. */
    @EventHandler
    fun handleQuit(event: PlayerQuitEvent) {
        EnvoyCompasses.deactivate(event.player)
    }

    private fun consumeOne(player: Player) {
        val item = player.inventory.itemInMainHand
        item.amount -= 1
    }
}
