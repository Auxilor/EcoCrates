package com.willfp.ecocrates.crate

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.util.ExactTestableItem
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

const val SCROLL_TIMES = 35
private val delays = listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 4, 5, 6, 10, 12, 25)

class Roll(
    private val rollItems: List<Reward>,
    val reward: Reward,
    private val crate: Crate,
    private val plugin: EcoPlugin,
    private val player: Player
) {
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

            if (scroll >= SCROLL_TIMES) {
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
                    ExactTestableItem(
                        ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE)
                            .setDisplayName("")
                            .build()
                    ),
                    ExactTestableItem(
                        ItemStackBuilder(Material.GREEN_STAINED_GLASS_PANE)
                            .setDisplayName("")
                            .build()
                    )
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

    fun roll() {
        gui.open(player)

        // Run the scroll animation
        runnable.runTaskTimer(1, 1)
    }

    private fun handleFinish(player: Player) {
        runnable.cancel()
        crate.handleFinish(player, this)
    }
}
