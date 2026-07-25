package com.willfp.ecocrates.crate.roll

import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.crate.placed.PlacedCrate
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.util.Vector

class RollCycle private constructor(
    override val reward: Reward,
    override val crate: Crate,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod,
    override val placedCrate: PlacedCrate?
) : Roll {
    private val duration = plugin.configYml.getInt("rolls.cycle.duration")
    private val wait = plugin.configYml.getInt("rolls.cycle.wait")
    private val interval = plugin.configYml.getInt("rolls.cycle.interval")
    private val heightOffset = plugin.configYml.getDouble("rolls.cycle.height-offset")
    private val display = crate.getRandomRewards(player, 100)

    // Line up with wherever the crate's preview item/hologram normally floats,
    // adjustable via rolls.cycle.height-offset.
    private val base = placedCrate?.location ?: location
    private val height = crate.randomRewardHeight + heightOffset

    private lateinit var item: Item

    override fun roll() {
        val world = location.world!!
        val hoverLocation = base.clone().add(Vector(0.0, height, 0.0))

        item = world.dropItem(hoverLocation, display[0].getDisplay(player, crate))
        item.velocity = Vector(0.0, 0.0, 0.0)
        item.pickupDelay = Int.MAX_VALUE
        item.setGravity(false)
        item.isCustomNameVisible = true
        item.setMetadata("ecocrates-roll-item", plugin.metadataValueFactory.create(true))
    }

    @Suppress("DEPRECATION")
    override fun tick(tick: Int) {
        val hoverLocation = base.clone().add(Vector(0.0, height, 0.0))
        item.velocity = Vector(0.0, 0.0, 0.0)
        item.teleport(hoverLocation)

        if (tick % interval == 0) {
            if (tick < duration) {
                val index = tick.floorDiv(interval).coerceAtMost(display.lastIndex)
                item.itemStack = display[index].getDisplay(player, crate)
                item.customName = display[index].displayName
            } else {
                item.itemStack = reward.getDisplay(player, crate)
                item.customName = reward.displayName
            }

            player.playSound(
                player.location,
                Sound.BLOCK_NOTE_BLOCK_PLING,
                1f,
                0.5f
            )
        }
    }

    override fun shouldContinueTicking(tick: Int): Boolean {
        return tick < wait + duration
    }

    override fun onFinish() {
        item.remove()
    }

    object Factory : RollFactory<RollCycle>("cycle") {
        override fun create(options: RollOptions): RollCycle =
            RollCycle(
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
