package com.willfp.ecocrates.crate.roll

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.util.NumberUtils
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.reward.Reward
import org.bukkit.Location
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.util.Vector

class RollSemiInstant private constructor(
    override val reward: Reward,
    override val crate: Crate,
    override val plugin: EcoPlugin,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
) : Roll {
    private val itemLifespan = plugin.configYml.getInt("rolls.semi_instant.item-lifespan")

    private val randomness = plugin.configYml.getDouble("rolls.semi_instant.velocity.randomness")

    private val velocity = Vector(
        plugin.configYml.getDouble("rolls.semi_instant.velocity.x"),
        plugin.configYml.getDouble("rolls.semi_instant.velocity.y"),
        plugin.configYml.getDouble("rolls.semi_instant.velocity.z")
    ).apply {
        this.x += NumberUtils.randFloat(-randomness, randomness)
        this.y += NumberUtils.randFloat(-randomness, randomness)
        this.z += NumberUtils.randFloat(-randomness, randomness)
    }

    private lateinit var item: Item

    override fun roll() {
        val world = location.world!!

        item = world.dropItem(location, reward.getDisplay(player, crate))
        item.pickupDelay = Int.MAX_VALUE
        item.isCustomNameVisible = true
        item.customName = reward.displayName

        player.closeInventory()

        item.velocity = velocity
    }

    override fun tick(tick: Int) {
        // No tick.
    }

    override fun shouldContinueTicking(tick: Int): Boolean {
        return false
    }

    override fun onFinish() {
        plugin.scheduler.runLater(itemLifespan.toLong()) {
            item.remove()
        }
    }

    object Factory : RollFactory<RollSemiInstant>("semi_instant") {
        override fun create(options: RollOptions): RollSemiInstant =
            RollSemiInstant(
                options.reward,
                options.crate,
                options.plugin,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}
