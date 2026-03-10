package com.willfp.ecocrates.util

import com.willfp.eco.core.drops.DropQueue
import com.willfp.ecocrates.crate.Crates
import com.willfp.ecocrates.crate.crate
import com.willfp.ecocrates.plugin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object CrateKeyListener : Listener {
    @EventHandler
    fun handle(event: BlockPlaceEvent) {
        if (event.itemInHand.crate != null) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun handleToGet(event: PlayerJoinEvent) {
        for (crate in Crates.values()) {
            val toGet = crate.getKeysToGet(event.player)
            if (toGet > 0) {
                val items = mutableListOf<ItemStack>().apply { repeat(toGet) { add(crate.key.item) } }

                if (plugin.configYml.getBool("track-player-keys")) {
                    items.forEach {
                        val meta = it.itemMeta!!
                        meta.persistentDataContainer.set(
                            plugin.namespacedKeyFactory.create("player"),
                            PersistentDataType.STRING,
                            event.player.uniqueId.toString()
                        )
                        it.itemMeta = meta
                    }
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
    }
}