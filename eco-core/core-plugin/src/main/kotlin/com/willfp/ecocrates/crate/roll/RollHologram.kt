package com.willfp.ecocrates.crate.roll

import com.willfp.eco.core.integrations.hologram.Hologram
import com.willfp.eco.core.integrations.hologram.HologramManager
import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.crate.placed.PlacedCrate
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.RewardSource
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.util.Vector

/**
 * A roll with no items at all - the crate's own hologram cycles through rewards
 * and locks onto the winner. Pairs with rolls.hologram.hide-placed-crate so the
 * crate's real hologram gets out of the way while this one plays. With several
 * rewards the hologram shows one block of lines per reward, locking in turn.
 */
class RollHologram private constructor(
    override val rewards: List<Reward>,
    override val source: RewardSource,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod,
    override val placedCrate: PlacedCrate?
) : Roll {
    override val reward: Reward = rewards.first()

    private val duration = plugin.configYml.getInt("rolls.hologram.duration")
    private val wait = plugin.configYml.getInt("rolls.hologram.wait")
    private val interval = plugin.configYml.getInt("rolls.hologram.interval").coerceAtLeast(1)
    private val height = plugin.configYml.getDouble("rolls.hologram.height")

    private val rollingLines = plugin.configYml.getFormattedStrings("rolls.hologram.rolling")
    private val winnerLines = plugin.configYml.getFormattedStrings("rolls.hologram.winner")
        .map { line -> line.replace("%rewards%", rewards.joinToString(", ") { it.displayName }) }

    private val blocks = rewards.mapIndexed { index, blockReward ->
        HologramBlock(blockReward, RewardStagger.lockTick(index = index, firstLock = duration, gap = wait))
    }

    // Sit where the crate's own hologram would, so the swap reads as one animation.
    private val base = placedCrate?.location ?: location

    private var hologram: Hologram? = null

    /**
     * One reward's block of lines, cycling through rewards until [lockTick] and then showing [blockReward].
     */
    private inner class HologramBlock(
        private val blockReward: Reward,
        val lockTick: Int
    ) {
        private val display = source.getRandomRewards(player, lockTick / interval + 1)

        val firstLines: List<String>
            get() = linesFor(rollingLines, display.first())

        fun isRolling(tick: Int): Boolean = tick < lockTick

        fun linesAt(tick: Int): List<String> =
            if (isRolling(tick)) {
                linesFor(rollingLines, display[tick.floorDiv(interval).coerceAtMost(display.lastIndex)])
            } else {
                linesFor(winnerLines, blockReward)
            }
    }

    override fun roll() {
        hologram = HologramManager.createHologram(
            base.clone().add(Vector(0.0, height, 0.0)),
            blocks.flatMap { it.firstLines }
        )
    }

    override fun tick(tick: Int) {
        val isIntervalTick = tick % interval == 0

        if (isIntervalTick || blocks.any { it.lockTick == tick }) {
            hologram?.setContents(blocks.flatMap { it.linesAt(tick) })
        }

        if (isIntervalTick) {
            if (blocks.any { it.isRolling(tick) }) {
                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.5f)
            } else {
                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.2f)
            }
        }
    }

    private fun linesFor(lines: List<String>, reward: Reward): List<String> {
        return lines.map { it.replace("%reward%", reward.displayName) }
    }

    override fun shouldContinueTicking(tick: Int): Boolean =
        tick < RewardStagger.endTick(count = rewards.size, firstLock = duration, gap = wait, hold = wait)

    override fun onFinish() {
        hologram?.remove()
        hologram = null
    }

    object Factory : RollFactory<RollHologram>("hologram") {
        override val maxRewards = 5

        override fun create(options: RollOptions): RollHologram =
            RollHologram(
                options.rewards,
                options.source,
                options.player,
                options.location,
                options.isReroll,
                options.method,
                options.placedCrate
            )
    }
}
