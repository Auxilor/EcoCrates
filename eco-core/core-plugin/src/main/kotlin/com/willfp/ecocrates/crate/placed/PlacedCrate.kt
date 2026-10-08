package com.willfp.ecocrates.crate.placed

import com.willfp.eco.core.Prerequisite
import com.willfp.eco.core.integrations.hologram.Hologram
import com.willfp.eco.core.integrations.hologram.HologramManager
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.runOwned
import com.willfp.ecocrates.util.CrateDisplayItems
import com.willfp.ecocrates.teleportCompat
import com.willfp.ecocrates.util.RollItems
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

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

    val chunkKey = if (Prerequisite.HAS_FOLIA.isMet) {
        Chunk.getChunkKey(location.blockX shr 4, location.blockZ shr 4)
    } else {
        location.chunk.key
    }

    @Volatile
    private var hologram: Hologram? = null

    @Volatile
    private var currentFrame: HologramFrame? = null

    @Volatile
    private var item: Item? = null

    // Players who should not see the preview hologram/item, e.g. while they're rolling.
    private val hiddenFrom: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    @Volatile
    private var removed = false

    internal fun tick(tick: Int) {
        if (removed) {
            return
        }

        tickRandomReward(tick)
        tickHolograms(tick)
    }

    internal fun tickAsync(tick: Int) {
        tickParticles(tick)
    }

    internal fun onRemove() {
        removed = true
        hologram?.remove()
        hologram = null
        item?.let { it.runOwned { it.remove() } }
        item = null
        hiddenFrom.clear()
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

    // Hologram and entity hide state both persist for the lifetime of what they're
    // hiding, so this is only needed when the preview item is replaced by a new entity.
    private fun hideNewItemFromHiddenPlayers() {
        val item = item ?: return

        for (uuid in hiddenFrom) {
            val player = Bukkit.getPlayer(uuid) ?: continue
            player.runOwned { player.hideEntity(plugin, item) }
        }
    }

    @Suppress("DEPRECATION")
    fun hideFrom(player: Player) {
        hiddenFrom.add(player.uniqueId)
        hologram?.hide(player)
        item?.let { player.hideEntity(plugin, it) }
    }

    @Suppress("DEPRECATION")
    fun showTo(player: Player) {
        hiddenFrom.remove(player.uniqueId)
        hologram?.show(player)
        item?.let { player.showEntity(plugin, it) }
    }

    @Suppress("DEPRECATION")
    private fun tickRandomReward(tick: Int) {
        if (!crate.isShowingRandomReward || crate.rewards.isEmpty()) {
            return
        }

        val world = location.world ?: return

        // Returns true if [item] now points at an entity it wasn't pointing at before.
        fun ensureItemSpawned(reward: com.willfp.ecocrates.reward.Reward): Boolean {
            val previous = item
            // clear the other items, but not roll animation items
            item?.let { item ->
                item.getNearbyEntities(0.5, 0.5, 0.5).filterIsInstance<Item>()
                    .filter { !it.hasGravity() && !RollItems.isRollItem(it) }
                    .forEach { it.remove() }
            }

            if (item == null) {
                val scan = world.getNearbyEntities(
                    location.clone().add(0.0, crate.randomRewardHeight, 0.0),
                    0.5, 0.5, 0.5
                ).filterIsInstance<Item>()
                    // Roll animation items float here too, and adopting one would
                    // both corrupt the roll and leave the crate without a preview.
                    .firstOrNull { !it.hasGravity() && !RollItems.isRollItem(it) }

                if (scan != null) {
                    CrateDisplayItems.mark(scan)
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
                CrateDisplayItems.mark(entity)
                item = entity
            }

            return item !== previous
        }

        if (tick % crate.randomRewardDelay == 0) {
            val reward = crate.rewards.random()

            /*
            Spawn item if item is gone
             */
            val isNewItem = ensureItemSpawned(reward)

            item?.itemStack = reward.getDisplay()
            item?.customName = crate.randomRewardName.replace("%reward%", reward.displayName)
            item?.isCustomNameVisible = true
            item?.teleportCompat(location.clone().add(0.0, crate.randomRewardHeight, 0.0))

            if (isNewItem) {
                hideNewItemFromHiddenPlayers()
            }
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
