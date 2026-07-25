package com.willfp.ecocrates.crate.roll

import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.util.lerp
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.util.Vector

/**
 * Short and loud: the item rises out of the crate flicking through rewards, hangs,
 * and a lightning strike reveals the winner. Lightning is cosmetic - nothing burns.
 */
class RollStrike private constructor(
    override val reward: Reward,
    override val crate: Crate,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
) : Roll {
    private val height = plugin.configYml.getDouble("rolls.strike.height")
    private val riseTime = plugin.configYml.getInt("rolls.strike.rise-time").coerceAtLeast(1)
    private val hangTime = plugin.configYml.getInt("rolls.strike.hang-time")
    private val revealTime = plugin.configYml.getInt("rolls.strike.reveal-time")
    private val interval = plugin.configYml.getInt("rolls.strike.interval").coerceAtLeast(1)

    private val display = crate.getRandomRewards(player, (riseTime / interval) + 1)

    private val strikeTick = riseTime + hangTime

    private lateinit var item: Item

    private var hasStruck = false

    @Suppress("DEPRECATION")
    override fun roll() {
        val world = location.world!!

        item = world.dropItem(location, display.first().getDisplay(player, crate))
        item.pickupDelay = Int.MAX_VALUE
        item.setGravity(false)
        item.isCustomNameVisible = true
        item.customName = display.first().displayName
        item.setMetadata("ecocrates-roll-item", plugin.metadataValueFactory.create(true))

        player.closeInventory()
    }

    @Suppress("DEPRECATION")
    override fun tick(tick: Int) {
        if (!item.isValid) {
            return
        }

        if (tick <= riseTime) {
            val target = location.clone().add(
                Vector(0.0, lerp(0.0, height, tick.toDouble() / riseTime), 0.0)
            )

            item.velocity = Vector(0, 0, 0)
            item.teleport(target)

            if (tick % interval == 0) {
                val next = display[tick.floorDiv(interval).coerceAtMost(display.lastIndex)]
                item.itemStack = next.getDisplay(player, crate)
                item.customName = next.displayName

                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.8f)
            }
        }

        if (tick >= strikeTick && !hasStruck) {
            strike()
        }
    }

    @Suppress("DEPRECATION")
    private fun strike() {
        hasStruck = true

        val world = location.world ?: return
        val impact = item.location

        world.strikeLightningEffect(location)
        world.spawnParticle(Particle.FLASH, impact, 3)
        world.spawnParticle(Particle.ELECTRIC_SPARK, impact, 40, 0.4, 0.4, 0.4, 0.15)

        player.playSound(player.location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 1.2f)

        item.itemStack = reward.getDisplay(player, crate)
        item.customName = reward.displayName
    }

    override fun shouldContinueTicking(tick: Int): Boolean {
        return tick < strikeTick + revealTime
    }

    override fun onFinish() {
        item.remove()
    }

    object Factory : RollFactory<RollStrike>("strike") {
        override fun create(options: RollOptions): RollStrike =
            RollStrike(
                options.reward,
                options.crate,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}
