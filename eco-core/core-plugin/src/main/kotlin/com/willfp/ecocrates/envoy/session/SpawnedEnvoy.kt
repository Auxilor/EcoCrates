package com.willfp.ecocrates.envoy.session

import com.willfp.eco.core.integrations.hologram.Hologram
import com.willfp.eco.core.integrations.hologram.HologramManager
import com.willfp.ecocrates.envoy.EnvoyCategory
import com.willfp.ecocrates.envoy.EnvoyRarity
import com.willfp.ecocrates.runOwned
import com.willfp.ecocrates.teleportCompat
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Item
import org.bukkit.util.Vector
import java.util.UUID

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

    /**
     * Identifies this crate's locator-bar waypoint. Not persisted - waypoints
     * are per-viewer and re-sent when a compass is activated, so a fresh ID
     * after a restart is harmless.
     */
    val waypointId: UUID = UUID.randomUUID()

    @Volatile
    private var hologram: Hologram? = null

    @Volatile
    private var item: Item? = null

    @Volatile
    private var despawned = false

    @Volatile
    private var fallingDisplay: BlockDisplay? = null

    private var fallOffset = 0.0

    /**
     * Places the block and starts the visuals. [withFireworks] is false when
     * restoring a session after a restart, so a reload doesn't re-fire every
     * firework in the session at once, and the crate is placed without falling.
     */
    fun place(withFireworks: Boolean) {
        val fall = category.fall

        if (!withFireworks || fall == null) {
            land(withFireworks)
            return
        }

        val world = blockLocation.world ?: return land(true)

        rarity.block.place(blockLocation)
        val blockData = blockLocation.block.blockData
        blockLocation.block.type = Material.AIR

        fallOffset = fall.height
        fallingDisplay = world.spawn(blockLocation.clone().add(0.0, fallOffset, 0.0), BlockDisplay::class.java) {
            it.block = blockData
            it.isPersistent = false
            it.teleportDuration = 1
        }
    }

    private fun land(withFireworks: Boolean) {
        rarity.block.place(blockLocation)

        if (withFireworks) {
            rarity.fireworks.spawn(centeredLocation)
            rarity.spawnSound?.playAt(centeredLocation)
        }
    }

    fun tick(tick: Int) {
        if (despawned) {
            return
        }

        if (tickFall()) {
            return
        }

        tickHologram()
        tickItemDisplay(tick)
    }

    /**
     * Moves a falling crate down one step. Returns true while it is still in the air.
     */
    private fun tickFall(): Boolean {
        val display = fallingDisplay ?: return false
        val fall = category.fall ?: return false

        fallOffset -= fall.speed

        if (fallOffset > 0) {
            display.teleportCompat(blockLocation.clone().add(0.0, fallOffset, 0.0))
            return true
        }

        display.remove()
        fallingDisplay = null
        land(true)
        return false
    }

    /**
     * Runs on the region owning the block, which also serialises it with [tick].
     */
    fun despawn() {
        blockLocation.runOwned {
            if (despawned) {
                return@runOwned
            }

            despawned = true

            hologram?.remove()
            hologram = null
            item?.remove()
            item = null
            fallingDisplay?.remove()
            fallingDisplay = null

            // The spawn locator guarantees the block was air before we placed it,
            // so clearing it back to air always restores the world.
            blockLocation.block.type = Material.AIR
        }
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
            teleportCompat(displayLocation)
        }
    }

    override fun toString() =
        "SpawnedEnvoy{category=${category.id},rarity=${rarity.id},location=$blockLocation}"
}
