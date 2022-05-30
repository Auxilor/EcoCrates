package com.willfp.ecocrates.util

import com.willfp.eco.core.EcoPlugin
import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.crate.placed.PlacedCrates
import org.bukkit.GameMode
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

        if (preventDoubles.contains(player.uniqueId)) {
            return
        }

        val crate = PlacedCrates.getCrateAt(block.location) ?: return

        val hasPhysicalKey = crate.hasPhysicalKey(player)
        val hasVirtualKey = crate.getVirtualKeys(player) > 0
        val openMethod = if (hasPhysicalKey) {
            OpenMethod.PHYSICAL_KEY
        } else if (hasVirtualKey) {
            OpenMethod.VIRTUAL_KEY
        } else {
            OpenMethod.MONEY
        }

        if (player.isSneaking && event.action == Action.RIGHT_CLICK_BLOCK) {
            event.isCancelled = true
            return
        }

        // Fix breaking
        if (player.gameMode == GameMode.CREATIVE && player.isSneaking && event.action == Action.LEFT_CLICK_BLOCK) {
            return
        }

        when (event.action) {
            Action.LEFT_CLICK_BLOCK -> crate.previewForPlayer(player)
            Action.RIGHT_CLICK_BLOCK -> crate.openPlaced(
                player,
                block.location,
                openMethod
            )
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
