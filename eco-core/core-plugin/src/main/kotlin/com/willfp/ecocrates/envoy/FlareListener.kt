package com.willfp.ecocrates.envoy

import com.willfp.ecocrates.envoy.session.EnvoySessions
import com.willfp.ecocrates.plugin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

/** Handles right-clicking an envoy flare item to manually start that flare's envoy category. */
object FlareListener : Listener {
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
        val categoryId = held.flareCategoryId ?: return
        val category = Envoys[categoryId] ?: return
        val flare = category.flare ?: return

        if (!flare.matches(held)) {
            return
        }

        event.isCancelled = true

        if (EnvoySessions.isActive()) {
            player.sendMessage(plugin.langYml.getMessage("envoy-already-active"))
            return
        }

        val cooldown = EnvoyCooldowns.remainingFlare(player, categoryId)

        if (cooldown > 0) {
            player.sendMessage(
                plugin.langYml.getMessage("flare-on-cooldown")
                    .replace("%seconds%", Math.ceil(cooldown / 20.0).toInt().toString())
            )
            return
        }

        // Check the price before consuming anything, so a player who can't
        // afford it keeps their flare.
        if (!flare.price.canAfford(player)) {
            player.sendMessage(
                plugin.langYml.getMessage("flare-cannot-afford")
                    .replace("%price%", flare.price.getDisplay(player))
            )
            return
        }

        if (!EnvoySessions.start(category)) {
            player.sendMessage(plugin.langYml.getMessage("envoy-start-failed"))
            return
        }

        flare.price.pay(player)
        consumeOne(player)
        EnvoyCooldowns.applyFlare(player, categoryId, flare.cooldownTicks)

        if (!flare.resetsSchedule) {
            // The category's own timer should keep running independently, so
            // undo the scheduler bookkeeping that start() just did.
            EnvoySessions.clearStartMark(categoryId)
        }

        player.sendMessage(
            plugin.langYml.getMessage("envoy-started")
                .withEnvoyPlaceholders(category)
                .replace("%amount%", EnvoySessions.remaining().toString())
        )
    }

    private fun consumeOne(player: Player) {
        val item = player.inventory.itemInMainHand
        item.amount -= 1
    }
}
