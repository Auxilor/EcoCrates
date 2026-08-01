package com.willfp.ecocrates.envoy.session

import com.willfp.eco.core.integrations.hologram.Hologram
import com.willfp.eco.core.integrations.hologram.HologramManager
import com.willfp.ecocrates.envoy.EnvoyCategory
import com.willfp.ecocrates.envoy.EnvoyRarity
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.util.Vector

/**
 * One envoy crate standing in the world.
 *
 * Unlike a placed crate, an envoy owns the block it stands on - it places it
 * on spawn and clears it back to air on despawn.
 */
class SpawnedEnvoy(
    val category: EnvoyCategory,
    val rarity: EnvoyRarity,
    val blockLocation: Location
) {
    val centeredLocation: Location = blockLocation.clone().apply {
        x = blockX + 0.5
        y = blockY + 0.5
        z = blockZ + 0.5
    }

    private var hologram: Hologram? = null

    private var item: Item? = null

    private var despawned = false

    /**
     * Places the block and starts the visuals. [withFireworks] is false when
     * restoring a session after a restart, so a reload doesn't re-fire every
     * firework in the session at once.
     */
    fun place(withFireworks: Boolean) {
        rarity.block.place(blockLocation)

        if (withFireworks) {
            rarity.fireworks.spawn(centeredLocation)
        }
    }

    fun tick(tick: Int) {
        if (despawned) {
            return
        }

        tickHologram()
        tickItemDisplay(tick)
    }

    fun despawn() {
        if (despawned) {
            return
        }

        despawned = true

        hologram?.remove()
        hologram = null
        item?.remove()
        item = null

        // The spawn locator guarantees the block was air before we placed it,
        // so clearing it back to air always restores the world.
        blockLocation.block.type = Material.AIR
    }

    private fun tickHologram() {
        if (hologram != null) {
            return
        }

        hologram = HologramManager.createHologram(
            centeredLocation.clone().add(0.0, rarity.hologramHeight, 0.0),
            listOf(rarity.hologramMessage)
        )
    }

    private fun tickItemDisplay(tick: Int) {
        if (!rarity.itemDisplayEnabled || rarity.rewards.isEmpty()) {
            return
        }

        if (tick % rarity.itemDisplayDelay != 0) {
            return
        }

        val world = centeredLocation.world ?: return
        val reward = rarity.rewards.random()
        val displayLocation = centeredLocation.clone().add(0.0, rarity.itemDisplayHeight, 0.0)

        val current = item

        if (current == null || current.isDead) {
            item = world.dropItem(displayLocation, reward.getDisplay()).apply {
                velocity = Vector(0.0, 0.0, 0.0)
                pickupDelay = Int.MAX_VALUE
                setGravity(false)
                isCustomNameVisible = true
            }
        }

        item?.apply {
            itemStack = reward.getDisplay()
            customName = rarity.itemDisplayName.replace("%reward%", reward.displayName)
            isCustomNameVisible = true
            teleport(displayLocation)
        }
    }

    override fun toString() =
        "SpawnedEnvoy{category=${category.id},rarity=${rarity.id},location=$blockLocation}"
}
