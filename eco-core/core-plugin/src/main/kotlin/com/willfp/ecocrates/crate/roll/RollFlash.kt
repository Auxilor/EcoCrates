package com.willfp.ecocrates.crate.roll

import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.RewardSource
import com.willfp.ecocrates.util.RollItems
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.concurrent.CopyOnWriteArrayList

private const val STOP_DISTANCE_IN_FRONT_OF_PLAYER = 1.5
private const val FLIGHT_SPEED = 0.5

class RollFlash private constructor(
    override val rewards: List<Reward>,
    override val source: RewardSource,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
) : Roll {
    override val reward: Reward = rewards.first()

    private val duration = plugin.configYml.getInt("rolls.flash.duration")
    private val wait = plugin.configYml.getInt("rolls.flash.wait")
    private val endTick = RewardStagger.endTick(count = rewards.size, firstLock = duration, gap = wait, hold = wait)

    private val flashItems = CopyOnWriteArrayList<FlashItem>()

    private var previousBlindness: PotionEffect? = null

    private var startTime = 0L

    /**
     * One reward's item, flying to its place in the line in front of the player,
     * cycling through rewards until its lock tick and then showing [itemReward].
     */
    private inner class FlashItem(
        private val itemReward: Reward,
        private val offset: LineOffset,
        val lockTick: Int
    ) {
        private val display = source.getRandomRewards(player, 100)

        val item: Item = location.world!!.dropItem(
            location.clone().add(offset.x, 0.0, offset.z),
            display[0].getDisplay(player, source)
        ).apply {
            pickupDelay = Int.MAX_VALUE
            setGravity(false)
            isCustomNameVisible = true
            RollItems.mark(this)
        }

        @Suppress("DEPRECATION")
        fun tick(tick: Int) {
            if (tick < duration) {
                item.velocity = player.eyeLocation.toVector()
                    .add(player.eyeLocation.direction.normalize().multiply(STOP_DISTANCE_IN_FRONT_OF_PLAYER))
                    .add(Vector(offset.x, 0.0, offset.z))
                    .subtract(item.location.toVector())
                    .multiply(tick.toDouble() / duration)
                    .multiply(FLIGHT_SPEED)
            } else {
                item.velocity = Vector(0, 0, 0)
            }

            if (tick < lockTick) {
                val index = tick.floorDiv(5).coerceAtMost(display.lastIndex)
                item.itemStack = display[index].getDisplay(player, source)
                item.customName = display[index].displayName
            } else {
                item.itemStack = itemReward.getDisplay(player, source)
                item.customName = itemReward.displayName
            }
        }
    }

    override fun roll() {
        val world = location.world!!

        RewardLine.offsets(count = rewards.size, yaw = player.location.yaw.toDouble())
            .zip(rewards)
            .forEachIndexed { index, (offset, itemReward) ->
                flashItems.add(
                    FlashItem(
                        itemReward,
                        offset,
                        RewardStagger.lockTick(index = index, firstLock = duration, gap = wait)
                    )
                )
            }

        player.closeInventory()

        previousBlindness = player.getPotionEffect(PotionEffectType.BLINDNESS)
        startTime = world.gameTime

        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.BLINDNESS,
                endTick * 2,
                1,
                false,
                false,
                false
            )
        )
    }

    override fun tick(tick: Int) {
        flashItems.filter { it.item.isValid && (tick % 5 == 0 || it.lockTick == tick) }.forEach { it.tick(tick) }

        if (tick % 4 == 0) {
            player.playSound(
                player.location,
                Sound.BLOCK_NOTE_BLOCK_PLING,
                1f,
                0.5f
            )
        }
    }

    override fun shouldContinueTicking(tick: Int): Boolean =
        tick < endTick

    override fun onFinish() {
        player.removePotionEffect(PotionEffectType.BLINDNESS)

        previousBlindness?.let { effect ->
            val remaining = effect.duration - (player.world.gameTime - startTime).toInt()
            when {
                effect.isInfinite -> player.addPotionEffect(effect)
                remaining > 0 -> player.addPotionEffect(effect.withDuration(remaining))
            }
        }

        flashItems.forEach { it.item.remove() }
    }

    object Factory : RollFactory<RollFlash>("flash") {
        override val maxRewards = 5

        override fun create(options: RollOptions): RollFlash =
            RollFlash(
                options.rewards,
                options.source,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}
