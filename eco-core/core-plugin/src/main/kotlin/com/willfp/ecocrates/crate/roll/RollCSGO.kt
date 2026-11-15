package com.willfp.ecocrates.crate.roll

import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.items.Items
import com.willfp.eco.util.NumberUtils
import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.crate.isOpeningCrate
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.RewardSource
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Scrolls rewards past a selector until one lands under it. With several rewards
 * there is one scrolling lane per reward, stacked between the two selector rows,
 * and the lanes stop in turn from the top down. Each lane stops with its reward
 * under the selector.
 */
class RollCSGO private constructor(
    override val rewards: List<Reward>,
    override val source: RewardSource,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
) : Roll {
    override val reward: Reward = rewards.first()

    private val scrollTimes = plugin.configYml.getInt("rolls.csgo.scrolls")
    private val bias = plugin.configYml.getDouble("rolls.csgo.bias")
    private val maxDelay = plugin.configYml.getInt("rolls.csgo.max-delay")

    private val lanes = rewards.mapIndexed { index, laneReward -> Lane(index, laneReward) }

    /**
     * One scrolling row of rewards that stops with [laneReward] under the selector.
     */
    private inner class Lane(index: Int, laneReward: Reward) {
        private val scrolls = GuiRollLayout.csgoLaneScrolls(lane = index, scrolls = scrollTimes)
        private val stop = GuiRollLayout.csgoLaneStop(lane = index, lanes = rewards.size, scrolls = scrollTimes)

        /*
        Bias the number using the NumberUtils bias function,
        but varying inputs from 0-35 rather than 0-1,
        and then multiplying the output by 25 to give
        a maximum tick value of 25 (rather than 1);
        essentially doing f(x/<scrolls>) * <max delay>.
         */
        private val delays = (1..scrolls + 1)
            .asSequence()
            .map { it / scrolls.toDouble() }
            .map { NumberUtils.bias(it, bias) }
            .map { it * maxDelay }
            .map { it.toInt() }
            // Make it 1!
            .map { if (it <= 0) 1 else it }
            .toList()

        // Add three so it lines up
        val display = source.getRandomRewards(player, scrolls + 3)
            .toMutableList()
            .apply {
                add(laneReward)
                addAll(source.getRandomRewards(player, 5))
            }

        var scroll = 0
        private var ticksSinceLastScroll = 0

        val isScrolling: Boolean
            get() = scroll < stop

        /**
         * Advances the lane by one tick.
         *
         * @return If the lane scrolled this tick.
         */
        fun tick(): Boolean {
            val scrolled = ticksSinceLastScroll % delays[scroll] == 0

            if (scrolled) {
                ticksSinceLastScroll = 0
                scroll++
            }

            ticksSinceLastScroll++

            return scrolled
        }
    }

    private val gui = menu(GuiRollLayout.csgoRows(lanes.size)) {
        setMask(
            FillerMask(
                MaskItems(
                    Items.lookup(plugin.configYml.getString("rolls.csgo.filler")),
                    Items.lookup(plugin.configYml.getString("rolls.csgo.selector"))
                ),
                *Array(GuiRollLayout.csgoRows(lanes.size)) { GuiRollLayout.csgoMaskRow(row = it, lanes = lanes.size) }
            )
        )

        title = source.name

        for ((index, lane) in lanes.withIndex()) {
            for (i in 1..9) {
                setSlot(
                    GuiRollLayout.csgoLaneRow(index),
                    i,
                    slot(
                        ItemStack(Material.AIR)
                    ) {
                        setUpdater { _, _, _ ->
                            lane.display[((9 - i) + lane.scroll).coerceAtMost(lane.display.lastIndex)]
                                .getDisplay(player, source)
                        }
                    }
                )
            }
        }

        onClose { _, _ ->
            player.isOpeningCrate = false
        }
    }

    override fun roll() {
        gui.open(player)
    }

    override fun tick(tick: Int) {
        if (lanes.filter { it.isScrolling }.map { it.tick() }.any { it }) {
            gui.refresh(player)

            player.playSound(
                player.location,
                Sound.BLOCK_STONE_BUTTON_CLICK_ON,
                1.0f,
                1.0f
            )
        }
    }

    override fun shouldContinueTicking(tick: Int): Boolean = lanes.any { it.isScrolling }

    override fun onFinish() {
        player.closeInventory()
    }

    object Factory : RollFactory<RollCSGO>("csgo") {
        override val isGuiRoll = true

        override val maxRewards = GuiRollLayout.CSGO_MAX_LANES

        override fun create(options: RollOptions): RollCSGO =
            RollCSGO(
                options.rewards,
                options.source,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}
