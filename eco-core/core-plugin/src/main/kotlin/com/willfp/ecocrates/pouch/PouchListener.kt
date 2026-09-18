package com.willfp.ecocrates.pouch

import com.willfp.ecocrates.plugin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

/** Right-click a held pouch to open it; sneak + right-click to preview it. */
object PouchListener : Listener {
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
        val pouchId = PouchItem.readPouchId(held) ?: return

        event.isCancelled = true

        val pouch = Pouches[pouchId]

        if (pouch == null) {
            player.sendMessage(plugin.langYml.getMessage("invalid-pouch"))
            return
        }

        if (player.isSneaking && pouch.previewOnShift) {
            pouch.previewForPlayer(player)
            return
        }

        pouch.open(player, held)
    }

    @EventHandler
    fun preventPlace(event: BlockPlaceEvent) {
        if (PouchItem.readPouchId(event.itemInHand) != null) {
            event.isCancelled = true
        }
    }
}
