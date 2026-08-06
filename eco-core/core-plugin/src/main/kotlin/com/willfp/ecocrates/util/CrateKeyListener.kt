package com.willfp.ecocrates.util

import com.willfp.eco.core.drops.DropQueue
import com.willfp.ecocrates.crate.Crates
import com.willfp.ecocrates.crate.isOpeningCrate
import com.willfp.ecocrates.crate.key
import com.willfp.ecocrates.envoy.EnvoyItems
import com.willfp.ecocrates.plugin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack

/**
 * Prevents crate key items from being placed as blocks, and hands out
 * offline-queued crate keys and envoy items when a player joins.
 */
object CrateKeyListener : Listener {
    @EventHandler
    fun handle(event: BlockPlaceEvent) {
        if (event.itemInHand.key != null) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun handleToGet(event: PlayerJoinEvent) {
        for (crate in Crates.values()) {
            val toGet = crate.getKeysToGet(event.player)
            if (toGet > 0) {
                val items = mutableListOf<ItemStack>().apply {
                    repeat(toGet) { add(crate.sharedKey.createItem(event.player)) }
                }

                crate.setKeysToGet(event.player, 0)

                DropQueue(event.player)
                    .addItems(items)
                    .forceTelekinesis()
                    .push()

                event.player.sendMessage(
                    plugin.langYml.getMessage("offline-keys-received")
                        .replace("%amount%", toGet.toString())
                        .replace("%crate%", crate.name)
                )
            }
        }

        EnvoyItems.grantPending(event.player)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        event.player.isOpeningCrate = false
    }
}