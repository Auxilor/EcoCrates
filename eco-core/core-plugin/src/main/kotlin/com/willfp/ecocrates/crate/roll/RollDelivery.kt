package com.willfp.ecocrates.crate.roll

import com.willfp.eco.core.entities.Entities
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector

/**
 * A courier entity is spawned at the crate holding the won reward, flies it over to
 * the player, and hands it off. The entity comes from eco's entity lookup, so it can
 * be any vanilla type or a custom entity from another plugin.
 */
class RollDelivery private constructor(
    override val reward: Reward,
    override val crate: Crate,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
) : Roll {
    private val courierEntity = Entities.lookup(plugin.configYml.getString("rolls.delivery.entity"))

    private val spawnDistance = plugin.configYml.getDouble("rolls.delivery.spawn-distance")
    private val spawnHeight = plugin.configYml.getDouble("rolls.delivery.spawn-height")
    private val speed = plugin.configYml.getDouble("rolls.delivery.speed").coerceAtLeast(0.01)
    private val hoverHeight = plugin.configYml.getDouble("rolls.delivery.hover-height")
    private val handOverTime = plugin.configYml.getInt("rolls.delivery.hand-over-time")
    private val timeout = plugin.configYml.getInt("rolls.delivery.timeout")

    private var courier: Entity? = null

    // Only used when the courier has no hands to put the reward in.
    private var carriedItem: Item? = null

    private var timeSpentHandingOver = 0
    private var hasArrived = false

    @Suppress("DEPRECATION")
    override fun roll() {
        val world = location.world!!

        // Start behind the crate, relative to the player, so the courier flies towards them.
        val awayFromPlayer = location.toVector()
            .subtract(player.location.toVector())
            .setY(0.0)

        val offset = if (awayFromPlayer.lengthSquared() < 0.01) {
            Vector(spawnDistance, 0.0, 0.0)
        } else {
            awayFromPlayer.normalize().multiply(spawnDistance)
        }

        val spawnLocation = location.clone()
            .add(offset)
            .add(0.0, spawnHeight, 0.0)

        val spawned = courierEntity.spawn(spawnLocation)

        spawned.isSilent = true
        spawned.isInvulnerable = true
        spawned.isPersistent = false
        spawned.setGravity(false)
        spawned.setMetadata("ecocrates-roll-item", plugin.metadataValueFactory.create(true))

        if (spawned is LivingEntity) {
            spawned.setAI(false)
            spawned.isCollidable = false
            spawned.canPickupItems = false
            spawned.removeWhenFarAway = true
            spawned.equipment?.setItemInMainHand(reward.getDisplay(player, crate))
        }

        // Entities without equipment carry the reward as a passenger instead.
        if (spawned !is LivingEntity || spawned.equipment == null) {
            val item = world.dropItem(spawnLocation, reward.getDisplay(player, crate))

            item.pickupDelay = Int.MAX_VALUE
            item.setGravity(false)
            item.isCustomNameVisible = true
            item.customName = reward.displayName
            item.setMetadata("ecocrates-roll-item", plugin.metadataValueFactory.create(true))

            spawned.addPassenger(item)
            carriedItem = item
        }

        courier = spawned

        player.closeInventory()
        player.playSound(player.location, Sound.ENTITY_ALLAY_ITEM_TAKEN, 1.0f, 1.0f)
    }

    override fun tick(tick: Int) {
        val courier = this.courier

        if (courier == null || !courier.isValid) {
            hasArrived = true
            timeSpentHandingOver = handOverTime + 1
            return
        }

        val target = player.location.clone().add(0.0, hoverHeight, 0.0)
        val delta = target.toVector().subtract(courier.location.toVector())

        if (delta.length() <= speed + 0.5) {
            if (!hasArrived) {
                hasArrived = true
                player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.2f)
            }

            timeSpentHandingOver++
            return
        }

        // Step towards the player and turn to face them as it goes.
        val next = courier.location.clone().add(delta.clone().normalize().multiply(speed))
        next.direction = delta

        courier.teleport(next)

        if (tick % 8 == 0) {
            player.playSound(player.location, Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 0.6f, 1.0f)
        }
    }

    override fun shouldContinueTicking(tick: Int): Boolean {
        return timeSpentHandingOver <= handOverTime && tick < timeout
    }

    override fun onFinish() {
        carriedItem?.remove()
        carriedItem = null

        courier?.remove()
        courier = null
    }

    object Factory : RollFactory<RollDelivery>("delivery") {
        override fun create(options: RollOptions): RollDelivery =
            RollDelivery(
                options.reward,
                options.crate,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}
