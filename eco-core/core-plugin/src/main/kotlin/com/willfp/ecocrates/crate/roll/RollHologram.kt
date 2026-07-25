package com.willfp.ecocrates.crate.roll

import com.willfp.eco.core.integrations.hologram.Hologram
import com.willfp.eco.core.integrations.hologram.HologramManager
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.crate.placed.PlacedCrate
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.util.Vector

/**
 * A roll with no items at all - the crate's own hologram cycles through rewards
 * and locks onto the winner. Pairs with rolls.hologram.hide-placed-crate so the
 * crate's real hologram gets out of the way while this one plays.
 */
class RollHologram private constructor(
    override val reward: Reward,
    override val crate: Crate,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod,
    override val placedCrate: PlacedCrate?
) : Roll {
    private val duration = plugin.configYml.getInt("rolls.hologram.duration")
    private val wait = plugin.configYml.getInt("rolls.hologram.wait")
    private val interval = plugin.configYml.getInt("rolls.hologram.interval").coerceAtLeast(1)
    private val height = plugin.configYml.getDouble("rolls.hologram.height")

    private val rollingLines = plugin.configYml.getStrings("rolls.hologram.rolling")
    private val winnerLines = plugin.configYml.getStrings("rolls.hologram.winner")

    private val display = crate.getRandomRewards(player, (duration / interval) + 1)

    // Sit where the crate's own hologram would, so the swap reads as one animation.
    private val base = placedCrate?.location ?: location

    private var hologram: Hologram? = null

    override fun roll() {
        hologram = HologramManager.createHologram(
            base.clone().add(Vector(0.0, height, 0.0)),
            linesFor(rollingLines, display.first())
        )
    }

    override fun tick(tick: Int) {
        if (tick % interval != 0) {
            return
        }

        if (tick < duration) {
            val index = tick.floorDiv(interval).coerceAtMost(display.lastIndex)
            hologram?.setContents(linesFor(rollingLines, display[index]))

            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.5f)
        } else {
            hologram?.setContents(linesFor(winnerLines, reward))

            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.2f)
        }
    }

    private fun linesFor(lines: List<String>, reward: Reward): List<String> {
        return lines.map { it.replace("%reward%", reward.displayName) }
    }

    override fun shouldContinueTicking(tick: Int): Boolean {
        return tick < wait + duration
    }

    override fun onFinish() {
        hologram?.remove()
        hologram = null
    }

    object Factory : RollFactory<RollHologram>("hologram") {
        override fun create(options: RollOptions): RollHologram =
            RollHologram(
                options.reward,
                options.crate,
                options.player,
                options.location,
                options.isReroll,
                options.method,
                options.placedCrate
            )
    }
}
