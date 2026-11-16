package com.willfp.ecocrates.crate.roll

import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.envoy.EnvoyFireworks
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.RewardSource
import com.willfp.ecocrates.teleportCompat
import com.willfp.ecocrates.util.RollItems
import com.willfp.ecocrates.util.lerp
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

private const val FLASH_COUNT = 3
private const val SPARK_COUNT = 40
private const val SPARK_SPREAD = 0.4
private const val SPARK_SPEED = 0.15
private const val THUNDER_VOLUME = 0.7f
private const val THUNDER_PITCH = 1.2f

/**
 * Short and loud: the item rises out of the crate flicking through rewards, hangs,
 * and a lightning strike reveals the winner. Lightning is cosmetic - nothing burns.
 * With several rewards the items rise side by side and are struck in turn, left to right.
 */
class RollStrike private constructor(
    override val rewards: List<Reward>,
    override val source: RewardSource,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
) : Roll {
    override val reward: Reward = rewards.first()

    private val height = plugin.configYml.getDouble("rolls.strike.height")
    private val riseTime = plugin.configYml.getInt("rolls.strike.rise-time").coerceAtLeast(1)
    private val hangTime = plugin.configYml.getInt("rolls.strike.hang-time")
    private val revealTime = plugin.configYml.getInt("rolls.strike.reveal-time")
    private val interval = plugin.configYml.getInt("rolls.strike.interval").coerceAtLeast(1)
    private val flashColor = EnvoyFireworks.parseColorOrNull(
        plugin.configYml.getString("rolls.strike.flash-color")
    ) ?: Color.WHITE

    private val strikeTick = riseTime + hangTime

    private val strikeItems = CopyOnWriteArrayList<StrikeItem>()

    /**
     * One reward's item, rising from [start] flicking through rewards until
     * its own lightning strike on [strikeAt] reveals [itemReward].
     */
    private inner class StrikeItem(
        private val itemReward: Reward,
        private val start: Location,
        private val strikeAt: Int
    ) {
        private val display = source.getRandomRewards(player, riseTime / interval + 1)

        private val hasStruck = AtomicBoolean(false)

        @Suppress("DEPRECATION")
        val item: Item = start.world!!.dropItem(start, display.first().getDisplay(player, source)).apply {
            pickupDelay = Int.MAX_VALUE
            setGravity(false)
            isCustomNameVisible = true
            customName = display.first().displayName
            RollItems.mark(this)
        }

        @Suppress("DEPRECATION")
        fun rise(tick: Int) {
            item.velocity = Vector(0, 0, 0)
            item.teleportCompat(
                start.clone().add(
                    Vector(0.0, lerp(start = 0.0, end = height, fraction = tick.toDouble() / riseTime), 0.0)
                )
            )

            if (tick % interval == 0) {
                val next = display[tick.floorDiv(interval).coerceAtMost(display.lastIndex)]
                item.itemStack = next.getDisplay(player, source)
                item.customName = next.displayName
            }
        }

        fun strikeIfDue(tick: Int) {
            if (tick >= strikeAt && hasStruck.compareAndSet(false, true)) {
                strike()
            }
        }

        @Suppress("DEPRECATION")
        private fun strike() {
            val world = start.world ?: return
            val impact = item.location

            world.strikeLightningEffect(start)
            world.spawnParticle(Particle.FLASH, impact, FLASH_COUNT, flashColor)
            world.spawnParticle(
                Particle.ELECTRIC_SPARK,
                impact,
                SPARK_COUNT,
                SPARK_SPREAD,
                SPARK_SPREAD,
                SPARK_SPREAD,
                SPARK_SPEED
            )

            player.playSound(player.location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, THUNDER_VOLUME, THUNDER_PITCH)

            item.itemStack = itemReward.getDisplay(player, source)
            item.customName = itemReward.displayName
        }
    }

    override fun roll() {
        RewardLine.offsets(count = rewards.size, yaw = player.location.yaw.toDouble())
            .zip(rewards)
            .forEachIndexed { index, (offset, itemReward) ->
                strikeItems.add(
                    StrikeItem(
                        itemReward,
                        location.clone().add(offset.x, 0.0, offset.z),
                        RewardStagger.lockTick(index = index, firstLock = strikeTick, gap = interval)
                    )
                )
            }

        player.closeInventory()
    }

    override fun tick(tick: Int) {
        val validItems = strikeItems.filter { it.item.isValid }

        if (tick <= riseTime) {
            validItems.forEach { it.rise(tick) }

            if (tick % interval == 0 && validItems.isNotEmpty()) {
                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.8f)
            }
        }

        validItems.forEach { it.strikeIfDue(tick) }
    }

    override fun shouldContinueTicking(tick: Int): Boolean =
        tick < RewardStagger.endTick(count = rewards.size, firstLock = strikeTick, gap = interval, hold = revealTime)

    override fun onFinish() {
        strikeItems.forEach { it.item.remove() }
    }

    object Factory : RollFactory<RollStrike>("strike") {
        override val maxRewards = 5

        override fun create(options: RollOptions): RollStrike =
            RollStrike(
                options.rewards,
                options.source,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}
