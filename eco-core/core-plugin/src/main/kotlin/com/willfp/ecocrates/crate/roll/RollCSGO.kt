package com.willfp.ecocrates.crate.roll

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.items.Items
import com.willfp.eco.util.NumberUtils
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.reward.Reward
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class RollCSGO(
    reward: Reward,
    private val rollItems: List<Reward>,
    private val crate: Crate,
    private val plugin: EcoPlugin,
    player: Player
) : Roll("csgo", player, reward) {
    private val scrollTimes = 35
    private val bias = plugin.configYml.getDouble("rolls.csgo.bias")
    private val delays = (1..scrollTimes)
        .asSequence()
        .map { it / scrollTimes.toDouble() }
        .map { NumberUtils.bias(it, bias) }
        .map { it * 25 }
        .map { it.toInt() }
        .map { if (it <= 0) 1 else it }  // Make it 1!
        .toList()

    private val display = rollItems.toMutableList().apply {
        add(reward) // Add the item that the player will win
        addAll(rollItems.shuffled()) // Add extra items for filler
    }

    private var scroll = 0
    private var tick = 0

    private val runnable = plugin.runnableFactory.create {
        val currentDelay = delays[scroll]

        if (tick % currentDelay == 0) {
            tick = 0
            scroll++

            gui.refresh(player)

            player.playSound(
                player.location,
                Sound.BLOCK_STONE_BUTTON_CLICK_ON,
                1.0f,
                1.0f
            )

            if (scroll >= scrollTimes) {
                it.cancel()
                plugin.scheduler.runLater(60) { player.closeInventory() }
            }
        }

        tick++
    }

    private val gui = menu(3) {
        setMask(
            FillerMask(
                MaskItems(
                    Items.lookup(plugin.configYml.getString("rolls.csgo.filler")),
                    Items.lookup(plugin.configYml.getString("rolls.csgo.selector"))
                ),
                "111121111",
                "000000000",
                "111121111"
            )
        )

        setTitle(crate.name)

        for (i in 1..9) {
            setSlot(
                2,
                i,
                slot(
                    ItemStack(Material.AIR)
                ) {
                    setUpdater { _, _, _ ->
                        display[(9 - i) + scroll].display
                    }
                }
            )
        }

        onClose { event, _ ->
            handleFinish(event.player as Player)
        }
    }

    override fun roll() {
        gui.open(player)

        // Run the scroll animation
        runnable.runTaskTimer(1, 1)
    }

    private fun handleFinish(player: Player) {
        runnable.cancel()
        crate.handleFinish(player, this)
    }
}