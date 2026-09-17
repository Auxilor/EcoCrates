package com.willfp.ecocrates.util

import com.willfp.ecocrates.plugin
import org.bukkit.entity.Entity
import org.bukkit.persistence.PersistentDataType

/**
 * Marks the item entities used for the idle display above a placed crate.
 *
 * Kept apart from [RollItems] because a display item is adopted again after a
 * restart rather than swept, and because the placed crate scan has to be able
 * to tell the two kinds apart.
 */
object CrateDisplayItems {
    const val METADATA_KEY = "ecocrates-display-item"

    private val pdcKey = plugin.namespacedKeyFactory.create("display_item")

    fun mark(entity: Entity) {
        entity.setMetadata(METADATA_KEY, plugin.metadataValueFactory.create(true))
        entity.persistentDataContainer.set(pdcKey, PersistentDataType.BYTE, 1)
    }

    fun isDisplayItem(entity: Entity): Boolean =
        entity.hasMetadata(METADATA_KEY) || entity.persistentDataContainer.has(pdcKey, PersistentDataType.BYTE)
}
