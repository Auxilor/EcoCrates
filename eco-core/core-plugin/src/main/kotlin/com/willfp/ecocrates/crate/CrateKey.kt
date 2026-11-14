package com.willfp.ecocrates.crate

import com.willfp.ecocrates.plugin
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import wiki.ecohub.game.item.FastItemStack
import wiki.ecohub.game.item.fast

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
