package com.willfp.ecocrates.crate

import com.willfp.eco.core.fast.FastItemStack
import com.willfp.eco.core.fast.fast
import com.willfp.ecocrates.plugin
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

private val CRATE_KEY = plugin.createNamespacedKey("key")

var ItemStack.crate: Crate?
    get() = this.fast().crate
    set(value) {
        this.fast().crate = value
    }

var FastItemStack.crate: Crate?
    get() = this.persistentDataContainer.crate
    set(value) {
        this.persistentDataContainer.crate = value
    }

var PersistentDataContainer.crate: Crate?
    get() {
        val id = this.get(CRATE_KEY, PersistentDataType.STRING) ?: return null
        return Crates[id]
    }
    set(value) {
        if (value == null) {
            this.remove(CRATE_KEY)
            return
        }

        this.set(CRATE_KEY, PersistentDataType.STRING, value.id)
    }
