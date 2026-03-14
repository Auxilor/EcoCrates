package com.willfp.ecocrates.crate

import com.willfp.eco.core.fast.FastItemStack
import com.willfp.eco.core.fast.fast
import com.willfp.ecocrates.plugin
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

private val KEY_ID = plugin.createNamespacedKey("key_id")

var ItemStack.key: SharedKey?
    get() = this.fast().key
    set(value) {
        this.fast().key = value
    }

var FastItemStack.key: SharedKey?
    get() = this.persistentDataContainer.key
    set(value) {
        this.persistentDataContainer.key = value
    }

var PersistentDataContainer.key: SharedKey?
    get() {
        val id = this.get(KEY_ID, PersistentDataType.STRING) ?: return null
        return Keys[id]
    }
    set(value) {
        if (value == null) {
            this.remove(KEY_ID)
            return
        }

        this.set(KEY_ID, PersistentDataType.STRING, value.id)
    }
