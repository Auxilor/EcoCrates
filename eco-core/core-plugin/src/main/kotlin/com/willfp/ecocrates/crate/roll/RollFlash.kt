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
    override val crate: Crate,
    override val plugin: EcoPlugin,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean
) : Roll {
    private val duration = plugin.configYml.getInt("rolls.flash.duration")
    private val wait = plugin.configYml.getInt("rolls.flash.wait")
    private val display = crate.getRandomRewards(player, 100, displayWeight = true)

    private lateinit var item: Item

    override fun roll() {
        val world = location.world!!

        item = world.dropItem(location, display[0].getDisplay(player, crate))
        item.pickupDelay = Int.MAX_VALUE
        item.setGravity(false)
        item.isCustomNameVisible = true

        player.closeInventory()

        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.BLINDNESS,
                (wait + duration) * 2,
                1,
                false,
                false,
                false
            )
        )
    }

    override fun tick(tick: Int) {
        if (tick % 5 == 0) {
            if (tick < duration) {
                item.velocity = player.eyeLocation.toVector()
                    .add(player.eyeLocation.direction.normalize().multiply(1.5)) // Make it stop in front of the player
                    .subtract(item.location.toVector())
                    .multiply(tick.toDouble() / duration)
                    .multiply(0.5)

                item.itemStack = display[tick.floorDiv(5)].getDisplay(player, crate)
                item.customName = display[tick.floorDiv(5)].displayName
            } else {
                item.itemStack = reward.getDisplay(player, crate)
                item.customName = reward.displayName
                item.velocity = Vector(0, 0, 0)
            }
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
        return tick < wait + duration
    }

    override fun onFinish() {
        player.removePotionEffect(PotionEffectType.BLINDNESS)
        item.remove()
    }

    object Factory : RollFactory<RollFlash>("flash") {
        override fun create(options: RollOptions): RollFlash =
            RollFlash(
                options.reward,
                options.crate,
                options.plugin,
                options.player,
                options.location,
                options.isReroll
            )
    }
}
