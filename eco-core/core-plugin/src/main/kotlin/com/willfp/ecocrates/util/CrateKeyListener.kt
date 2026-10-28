package com.willfp.ecocrates.util

import com.willfp.eco.core.data.profile
import com.willfp.eco.core.drops.DropQueue
import com.willfp.ecocrates.crate.Keys
import com.willfp.ecocrates.crate.isOpeningCrate
import com.willfp.ecocrates.crate.key
import com.willfp.ecocrates.envoy.EnvoyItems
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.pouch.Pouches
import com.willfp.ecocrates.reward.PendingRewards
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack

/**
 * Prevents crate key items from being placed as blocks, and hands out
 * offline-queued crate keys, envoy items and crate rewards when a player joins.
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
        grantPending(event.player)
    }

    /** Hands [player] everything queued for them while they were offline. */
    fun grantPending(player: Player) {
        for (key in Keys.values()) {
            val toGet = player.profile.takePending(key.toGetKey)
            if (toGet > 0) {
                val items = mutableListOf<ItemStack>().apply {
                    repeat(toGet) { add(key.createItem(player)) }
                }

                DropQueue(player)
                    .addItems(items)
                    .forceTelekinesis()
                    .push()

                player.sendMessage(
                    plugin.langYml.getMessage("offline-keys-received")
                        .replace("%amount%", toGet.toString())
                        .replace("%crate%", key.displayName)
                )
            }
        }

        for (pouch in Pouches.values()) {
            pouch.item.grantPending(player)
        }

        EnvoyItems.grantPending(player)
        PendingRewards.grantPending(player)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        event.player.isOpeningCrate = false
    }
}