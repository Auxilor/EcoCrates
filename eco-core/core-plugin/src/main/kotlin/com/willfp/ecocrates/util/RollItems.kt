package com.willfp.ecocrates.util

import com.willfp.ecocrates.plugin
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

/**
 * Marks the item entities used for roll animations.
 *
 * The marker is written to persistent data as well as metadata, tagged with
 * the id of the server session that spawned it, because metadata does not
 * survive a restart and an orphan has to be told apart from a running roll.
 */
object RollItems {
    const val METADATA_KEY = "ecocrates-roll-item"

    private val pdcKey = plugin.namespacedKeyFactory.create("roll_item_session")

    private val sessionId = UUID.randomUUID().toString()

    fun mark(entity: Entity) {
        entity.setMetadata(METADATA_KEY, plugin.metadataValueFactory.create(true))
        entity.persistentDataContainer.set(pdcKey, PersistentDataType.STRING, sessionId)
    }

    fun isRollItem(entity: Entity): Boolean =
        entity.hasMetadata(METADATA_KEY) || entity.persistentDataContainer.has(pdcKey, PersistentDataType.STRING)

    /** Whether [entity] is a roll item left behind by a previous server session. */
    fun isOrphaned(entity: Entity): Boolean {
        val session = entity.persistentDataContainer.get(pdcKey, PersistentDataType.STRING) ?: return false
        return session != sessionId
    }

    fun sweepLoadedChunks() {
        for (world in Bukkit.getWorlds()) {
            world.entities
                .filter { isOrphaned(it) }
                .forEach { it.remove() }
        }
    }
}
