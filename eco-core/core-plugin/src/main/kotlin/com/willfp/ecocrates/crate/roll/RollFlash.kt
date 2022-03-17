package com.willfp.ecocrates.crate.roll

import com.willfp.eco.core.EcoPlugin
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.reward.Reward
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector

class RollFlash private constructor(
    override val reward: Reward,
    private val crate: Crate,
    private val plugin: EcoPlugin,
    private val player: Player,
    private val location: Location
) : Roll {
    private val wait = plugin.configYml.getInt("rolls.flash.wait")
    private val display = crate.getRandomRewards(player, 100, displayWeight = true)

    private lateinit var item: Item

    override fun roll() {
        val world = location.world!!

        item = world.dropItem(location, display[0].display)
        item.pickupDelay = Int.MAX_VALUE
        item.setGravity(false)
        item.isCustomNameVisible = true

        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.BLINDNESS,
                wait * 2,
                1,
                false,
                false,
                false
            )
        )
    }

    override fun tick(tick: Int) {
        if (tick % 5 == 0) {
            item.velocity = player.eyeLocation.toVector()
                .add(player.eyeLocation.direction.normalize().multiply(1.5)) // Make it stop in front of the player
                .subtract(item.location.toVector())
                .multiply(tick.toDouble() / wait)
                .multiply(0.5)

            item.itemStack = display[tick.floorDiv(4)].display
            item.customName = display[tick.floorDiv(4)].displayName
        }

        if (tick % 4 == 0) {
            player.playSound(
                player.location,
                Sound.BLOCK_NOTE_BLOCK_PLING,
                1f,
                0.5f
            )
        }
    }

    override fun shouldContinueTicking(tick: Int): Boolean {
        return tick < wait
    }

    override fun onFinish() {
        player.removePotionEffect(PotionEffectType.BLINDNESS)

        player.playSound(
            player.location,
            Sound.ENTITY_FIREWORK_ROCKET_TWINKLE,
            1f,
            1f
        )

        item.itemStack = reward.display
        item.customName = reward.displayName
        item.velocity = Vector(0, 0, 0)

        plugin.scheduler.runLater(80) {
            item.remove()
        }
    }

    object Factory : RollFactory<RollFlash>("flash") {
        override fun create(options: RollOptions): RollFlash =
            RollFlash(
                options.reward,
                options.crate,
                options.plugin,
                options.player,
                options.location
            )
    }
}
