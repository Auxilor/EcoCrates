package com.willfp.ecocrates.crate.placed

import com.willfp.eco.core.integrations.hologram.Hologram
import com.willfp.eco.core.integrations.hologram.HologramManager
import com.willfp.ecocrates.crate.Crate
import org.bukkit.Location
import org.bukkit.entity.Item
import org.bukkit.event.Cancellable
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

    private val particleCancellables: List<Cancellable> = crate.particles.map { it.spawn(location) }

    internal fun tick(tick: Int) {
        tickRandomReward(tick)
        tickHolograms(tick)
    }

    internal fun onRemove() {
        hologram?.remove()
        hologram = null
        item?.remove()
        item = null
        particleCancellables.forEach { it.setCancelled(true) }
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
                    .forEach { it.remove() }
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
            item?.teleport(location.clone().add(0.0, crate.randomRewardHeight, 0.0))
        }
    }

    override fun toString(): String {
        return "PlacedCrate{crate=$crate,location=$location}"
    }
}
