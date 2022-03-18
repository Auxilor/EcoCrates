package com.willfp.ecocrates.crate

import com.willfp.ecocrates.EcoCratesPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
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
    val meta = this.itemMeta ?: return null
    val pdc = meta.persistentDataContainer
    val id = pdc.get(key, PersistentDataType.STRING) ?: return null
    return Crates.getByID(id)
}

class CrateKeyListener: Listener {
    @EventHandler
    fun handle(event: BlockPlaceEvent) {
        if (event.itemInHand.getAsKey() != null) {
            event.isCancelled = true
        }
    }
}
