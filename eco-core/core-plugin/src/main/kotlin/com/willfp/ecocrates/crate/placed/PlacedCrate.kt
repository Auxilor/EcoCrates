package com.willfp.ecocrates.crate.placed

import com.willfp.eco.core.Prerequisite
import com.willfp.eco.core.integrations.hologram.Hologram
import com.willfp.eco.core.integrations.hologram.HologramManager
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.plugin
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

    private var hologram: Hologram? = null

    private var currentFrame: HologramFrame? = null

    private var item: Item? = null

    internal fun tick(tick: Int) {
        plugin.scheduler.runTask(location) { // folia issue
            tickRandomReward(tick)
            tickHolograms(tick)
        }
    }

    internal fun tickAsync(tick: Int) {
        tickParticles(tick)
    }

    internal fun onRemove() {
        hologram?.let {
            it.remove()
            hologram = null
        }
        item?.let {
            plugin.scheduler.runTask(it) {
                it.remove()
                item = null
            }
        }
    }

    private fun tickHolograms(tick: Int) {
        if (hologram == null) {
            hologram = HologramManager.createHologram(
                location.clone().add(0.0, crate.hologramHeight, 0.0),
                crate.hologramFrames.firstOrNull()?.lines ?: emptyList()
            )
        }

        var frameToShow: HologramFrame? = null

        for (hologramFrame in crate.hologramFrames) {
            if (hologramFrame.tick < (tick % crate.hologramTicks)) {
                frameToShow = hologramFrame
            }
        }

        if (currentFrame != frameToShow && frameToShow != null) {
            currentFrame = frameToShow
            @Suppress("USELESS_ELVIS")
            hologram?.setContents(frameToShow.lines ?: emptyList())
        }
    }

    @Suppress("DEPRECATION")
    private fun tickRandomReward(tick: Int) {
        if (!crate.isShowingRandomReward || crate.rewards.isEmpty()) {
            return
        }

        val world = location.world ?: return

        fun ensureItemSpawned(reward: com.willfp.ecocrates.reward.Reward) {
            // clear the other items, but not roll animation items
            item?.let { item ->
                item.getNearbyEntities(0.5, 0.5, 0.5).filterIsInstance<Item>()
                    .filter { !it.hasGravity() && !it.hasMetadata("ecocrates-roll-item") }
                    .forEach { plugin.scheduler.runTask(it) { it.remove() } }
            }

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
                    reward.getDisplay()
                )
                entity.velocity = Vector(0.0, 0.0, 0.0)
                entity.pickupDelay = Int.MAX_VALUE
                entity.setGravity(false)
                entity.isCustomNameVisible = true
                entity.customName = crate.randomRewardName.replace("%reward%", reward.displayName)
                entity.isPersistent = false
                item = entity
            }
        }

        if (tick % crate.randomRewardDelay == 0) {
            val reward = crate.rewards.random()

            /*
            Spawn item if item is gone
             */
            ensureItemSpawned(reward)

            item?.itemStack = reward.getDisplay()
            item?.customName = crate.randomRewardName.replace("%reward%", reward.displayName)
            item?.isCustomNameVisible = true
            val location = location.clone().add(0.0, crate.randomRewardHeight, 0.0)
            if (Prerequisite.HAS_PAPER.isMet)
                item?.teleportAsync(location)
            else
                item?.teleport(location) // damn spigot!
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
