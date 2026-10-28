package com.willfp.ecocrates.util

import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.crate.placed.PlacedCrates
import com.willfp.ecocrates.plugin
import org.bukkit.GameMode
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerInteractEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object PlacedCrateListener : Listener {
    // Janky fix to interact events firing twice
    private val preventDoubles: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    @EventHandler
    fun handleClick(event: PlayerInteractEvent) {
        val player = event.player
        val block = event.clickedBlock ?: return

        if (!preventDoubles.add(player.uniqueId)) {
            return
        }

        try {
            clickPlacedCrate(event, player, block)
        } finally {
            plugin.scheduler.on(player)
                .onRetired { preventDoubles.remove(player.uniqueId) }
                .run { preventDoubles.remove(player.uniqueId) }
        }
    }

    private fun clickPlacedCrate(event: PlayerInteractEvent, player: Player, block: Block) {
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

        // Fix breaking
        val isCreativeBreak = player.gameMode == GameMode.CREATIVE
            && player.isSneaking
            && event.action == Action.LEFT_CLICK_BLOCK

        if (player.isSneaking && event.action == Action.RIGHT_CLICK_BLOCK) {
            event.isCancelled = true

            if (hasPhysicalKey && crate.isShiftRightClickOpenAllEnabled) {
                crate.openPlacedAll(player, block.location, OpenMethod.PHYSICAL_KEY)
            }
        } else if (!isCreativeBreak) {
            when (event.action) {
                Action.LEFT_CLICK_BLOCK -> crate.previewForPlayer(player)
                Action.RIGHT_CLICK_BLOCK -> crate.openPlaced(player, block.location, openMethod)
                Action.LEFT_CLICK_AIR, Action.RIGHT_CLICK_AIR, Action.PHYSICAL -> return
            }

            event.isCancelled = true
        }
    }

    @EventHandler
    fun handleBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block

        PlacedCrates.getCrateAt(block.location) ?: return

        event.isCancelled = true

        if (!player.hasPermission("ecocrates.break") || !player.isSneaking) {
            player.sendMessage(plugin.langYml.getMessage("cannot-break-crate"))
            return
        }

        PlacedCrates.removeCrate(block.location)
        player.sendMessage(plugin.langYml.getMessage("removed-crate"))
    }

    @EventHandler(ignoreCancelled = true)
    fun handlePistonExtend(event: BlockPistonExtendEvent) {
        if (event.blocks.any { it.isPlacedCrate() }) {
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun handlePistonRetract(event: BlockPistonRetractEvent) {
        if (event.blocks.any { it.isPlacedCrate() }) {
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun handleEntityExplode(event: EntityExplodeEvent) {
        event.blockList().removeIf { it.isPlacedCrate() }
    }

    @EventHandler(ignoreCancelled = true)
    fun handleBlockExplode(event: BlockExplodeEvent) {
        event.blockList().removeIf { it.isPlacedCrate() }
    }

    @EventHandler(ignoreCancelled = true)
    fun handleBurn(event: BlockBurnEvent) {
        if (event.block.isPlacedCrate()) {
            event.isCancelled = true
        }
    }

    private fun Block.isPlacedCrate(): Boolean =
        PlacedCrates.getCrateAt(this.location) != null
}
