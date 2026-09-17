package com.willfp.ecocrates.util

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryPickupItemEvent
import org.bukkit.event.world.ChunkLoadEvent

/**
 * Keeps crate item entities under the plugin's control: removes roll display
 * items left behind by a previous server session, for the chunks that the
 * enable-time sweep could not reach, and stops containers taking either a roll
 * item or a placed crate display item, both of which carry a real reward
 * ItemStack and ignore the pickup delay that keeps players off them.
 */
object RollItemListener : Listener {
    @EventHandler
    fun onChunkLoad(event: ChunkLoadEvent) {
        event.chunk.entities
            .filter { RollItems.isOrphaned(it) }
            .forEach { it.remove() }
    }

    @EventHandler
    fun onContainerPickup(event: InventoryPickupItemEvent) {
        if (RollItems.isRollItem(event.item) || CrateDisplayItems.isDisplayItem(event.item)) {
            event.isCancelled = true
        }
    }
}
