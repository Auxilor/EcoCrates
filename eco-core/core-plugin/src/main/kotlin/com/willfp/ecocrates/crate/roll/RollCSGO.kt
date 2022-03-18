package com.willfp.ecocrates.crate.roll

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.items.Items
import com.willfp.eco.util.NumberUtils
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.isOpeningCrate
import com.willfp.ecocrates.reward.Reward
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class RollCSGO private constructor(
    override val reward: Reward,
    override val crate: Crate,
    override val plugin: EcoPlugin,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean
) : Roll {
    private val scrollTimes = plugin.configYml.getInt("rolls.csgo.scrolls")
    private val bias = plugin.configYml.getDouble("rolls.csgo.bias")
    private val maxDelay = plugin.configYml.getInt("rolls.csgo.max-delay")

    /*
    Bias the number using the NumberUtils bias function,
    but varying inputs from 0-35 rather than 0-1,
    and then multiplying the output by 25 to give
    a maximum tick value of 25 (rather than 1);
    essentially doing f(x/<scrolls>) * <max delay>.
     */
    private val delays = (1..scrollTimes + 1)
        .asSequence()
        .map { it / scrollTimes.toDouble() }
        .map { NumberUtils.bias(it, bias) }
        .map { it * maxDelay }
        .map { it.toInt() }
        .map { if (it <= 0) 1 else it }  // Make it 1!
        .toList()

    // Add three so it lines up
    private val display = crate.getRandomRewards(player, scrollTimes + 3, displayWeight = true)
        .toMutableList().apply {
            add(reward)
            addAll(crate.getRandomRewards(player, 5, displayWeight = true))
        }

    private var scroll = 0
    private var ticksSinceLastScroll = 0

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
                        display[(9 - i) + scroll].getDisplay(player, crate)
                    }
                }
            )
        }

        onClose { _, _ ->
            player.isOpeningCrate = false
        }
    }

    override fun roll() {
        gui.open(player)
    }

    override fun tick(tick: Int) {
        val currentDelay = delays[scroll]

        if (ticksSinceLastScroll % currentDelay == 0) {
            ticksSinceLastScroll = 0
            scroll++

            gui.refresh(player)

            player.playSound(
                player.location,
                Sound.BLOCK_STONE_BUTTON_CLICK_ON,
                1.0f,
                1.0f
            )
        }

        ticksSinceLastScroll++
    }

    override fun shouldContinueTicking(tick: Int): Boolean {
        return scroll <= scrollTimes
    }

    override fun onFinish() {
        player.closeInventory()
    }

    object Factory : RollFactory<RollCSGO>("csgo") {
        override fun create(options: RollOptions): RollCSGO =
            RollCSGO(
                options.reward,
                options.crate,
                options.plugin,
                options.player,
                options.location,
                options.isReroll
            )
    }
}
