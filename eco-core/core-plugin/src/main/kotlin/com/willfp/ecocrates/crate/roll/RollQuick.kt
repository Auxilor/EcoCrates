package com.willfp.ecocrates.crate.roll

import com.willfp.eco.core.EcoPlugin
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.reward.Reward
import org.bukkit.Location
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.util.Vector

class RollQuick private constructor(
    override val reward: Reward,
    override val crate: Crate,
    override val plugin: EcoPlugin,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean
) : Roll {
    private val riseVelocity = plugin.configYml.getDouble("rolls.quick.rise-velocity")
    private val height = plugin.configYml.getDouble("rolls.quick.height")
    private val suspend = plugin.configYml.getInt("rolls.quick.suspend")
    private lateinit var item: Item
    private var done = false
    private var suspendTicks = 0

    private val end = location.toVector()
        .add(Vector(0.0, height, 0.0))

    override fun roll() {
        val world = location.world!!

        item = world.dropItem(location, reward.getDisplay(player, crate))
        item.pickupDelay = Int.MAX_VALUE
        item.setGravity(false)
        item.isCustomNameVisible = true
        item.customName = reward.displayName

        player.closeInventory()
    }

    override fun tick(tick: Int) {
        if (item.location.toVector().distance(end) < 0.1) {
            item.teleport(end.toLocation(item.world))
            item.velocity = Vector(0, 0, 0)
            suspendTicks++

            if (suspendTicks >= suspend) {
                done = true
            }
        } else {
            val velocity = end.clone()
                .subtract(item.location.toVector())
                .normalize()
                .multiply(riseVelocity)

            item.velocity = velocity
        }
    }

    override fun shouldContinueTicking(tick: Int): Boolean {
        return !done
    }

    override fun onFinish() {
        item.remove()
    }

    object Factory : RollFactory<RollQuick>("quick") {
        override fun create(options: RollOptions): RollQuick =
            RollQuick(
                options.reward,
                options.crate,
                options.plugin,
                options.player,
                options.location,
                options.isReroll
            )
    }
}
