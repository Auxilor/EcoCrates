package com.willfp.ecocrates.crate.placed

import com.willfp.eco.core.integrations.hologram.HologramManager
import com.willfp.ecocrates.crate.Crate
import org.bukkit.Location
import org.bukkit.entity.Item
import org.bukkit.util.Vector

class PlacedCrate(
    val crate: Crate,
    blockLocation: Location
) {
    // Center the location, they're mutable because bukkit is bad at designing APIs.
    val location = blockLocation.clone().apply {
        x += 0.5
        y += 0.5
        z += 0.5
    }

    val chunkKey = location.chunk.key

    private val world = location.world!!

    private val hologram = HologramManager.createHologram(
        location.clone().add(0.0, crate.hologramHeight, 0.0),
        crate.hologramFrames.firstOrNull()?.lines ?: emptyList()
    )

    private var currentFrame: HologramFrame? = null

    private var item: Item? = null

    internal fun tick(tick: Int) {
        tickRandomReward(tick)
        tickHolograms(tick)
    }

    internal fun tickAsync(tick: Int) {
        tickParticles(tick)
    }

    internal fun onRemove() {
        hologram.remove()
        item?.remove()
    }

    internal fun handleChunkUnload() {
        hologram.remove()
        item?.remove()
    }

    private fun tickHolograms(tick: Int) {
        var frameToShow: HologramFrame? = null

        for (hologramFrame in crate.hologramFrames) {
            if (hologramFrame.tick < (tick % crate.hologramTicks)) {
                frameToShow = hologramFrame
            }
        }

        if (currentFrame != frameToShow && frameToShow != null) {
            currentFrame = frameToShow
            @Suppress("USELESS_ELVIS")
            hologram.setContents(frameToShow.lines ?: emptyList())
        }
    }

    private fun tickRandomReward(tick: Int) {
        if (!crate.isShowingRandomReward || crate.rewards.isEmpty()) {
            return
        }

        fun ensureItemSpawned() {
            if (item == null) {
                val scan = world.getNearbyEntities(
                    location.clone().add(0.0, crate.randomRewardHeight, 0.0),
                    0.5, 0.5, 0.5
                ).filterIsInstance<Item>().firstOrNull { !it.hasGravity() }

                if (scan != null) {
                    item = scan
                }
            }

            if (item == null || item?.isDead == true) {
                val entity = world.dropItem(
                    location.clone().add(0.0, crate.randomRewardHeight, 0.0),
                    crate.rewards.first().getDisplay()
                )
                entity.velocity = Vector(0.0, 0.0, 0.0)
                entity.pickupDelay = Int.MAX_VALUE
                entity.setGravity(false)
                entity.isCustomNameVisible = true
                entity.customName = crate.randomRewardName
                item = entity
            }
        }

        if (tick % crate.randomRewardDelay == 0) {
            /*
            Spawn item if item is gone
             */
            ensureItemSpawned()

            item?.itemStack = crate.rewards.random().getDisplay()
            item?.teleport(location.clone().add(0.0, crate.randomRewardHeight, 0.0))
        }
    }

    private fun tickParticles(tick: Int) {
        for ((particle, animation) in crate.particles.toList()) { // Anti ConcurrentModification
            animation.spawnParticle(location, tick, particle)
        }
    }

    override fun toString(): String {
        return "PlacedCrate{crate=$crate,location=$location}"
    }
}
