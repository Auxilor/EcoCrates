package com.willfp.ecocrates.crate

import com.willfp.eco.core.drops.DropQueue
import com.willfp.ecocrates.EcoCratesPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

private val plugin = EcoCratesPlugin.instance
private val key = plugin.namespacedKeyFactory.create("key")

fun ItemStack.setAsKeyFor(crate: Crate) {
    val meta = this.itemMeta ?: return
    val pdc = meta.persistentDataContainer
    pdc.set(key, PersistentDataType.STRING, crate.id)
    this.itemMeta = meta
}

fun ItemStack.getAsKey(): Crate? {
    for (crate in Crates.values()) {
        if (!crate.keyIsCustomItem) {
            continue
        }

        if (crate.key.matches(this)) {
            return crate
        }
    }

    val meta = this.itemMeta ?: return null
    val pdc = meta.persistentDataContainer
    val id = pdc.get(key, PersistentDataType.STRING) ?: return null
    return Crates.getByID(id)
}

class CrateKeyListener : Listener {
    @EventHandler
    fun handle(event: BlockPlaceEvent) {
        if (event.itemInHand.getAsKey() != null) {
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
                    items.map {
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
                    plugin.langYml.getMessage("  offline-keys-received")
                        .replace("%amount%", toGet.toString())
                        .replace("%crate%", crate.name)
                )
            }
        }
    }
}
