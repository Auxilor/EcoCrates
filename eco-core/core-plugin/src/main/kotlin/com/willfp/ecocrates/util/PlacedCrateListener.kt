package com.willfp.ecocrates.util

import com.willfp.eco.core.EcoPlugin
import com.willfp.ecocrates.crate.placed.PlacedCrates
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import java.util.*

class PlacedCrateListener(
    private val plugin: EcoPlugin
) : Listener {
    // Janky fix to interact events firing twice
    private val preventDoubles = mutableSetOf<UUID>()

    @EventHandler
    fun handleClick(event: PlayerInteractEvent) {
        val player = event.player
        val block = event.clickedBlock ?: return

        if (player.isSneaking) {
            return
        }

        if (preventDoubles.contains(player.uniqueId)) {
            return
        }

        val crate = PlacedCrates.getCrateAt(block.location) ?: return

        when (event.action) {
            Action.LEFT_CLICK_BLOCK -> crate.previewForPlayer(player)
            Action.RIGHT_CLICK_BLOCK -> crate.openPhysical(player, block.location)
            else -> return
        }

        preventDoubles.add(player.uniqueId)
        plugin.scheduler.run { preventDoubles.remove(player.uniqueId) }

        event.isCancelled = true
    }

    @EventHandler
    fun handleBreak(event: BlockBreakEvent) {
        val player = event.player

        if (!player.hasPermission("ecocrates.break")) {
            return
        }

        val block = event.block

        if (!player.isSneaking) {
            return
        }

        PlacedCrates.getCrateAt(block.location) ?: return

        PlacedCrates.removeCrate(block.location)
        player.sendMessage(plugin.langYml.getMessage("removed-crate"))
        event.isCancelled = true
    }
}
