package com.willfp.ecocrates.crate.roll

import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.items.Items
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.crate.isOpeningCrate
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class RollElimination private constructor(
    override val reward: Reward,
    override val crate: Crate,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
) : Roll {
    // Seven is the widest row that still leaves a border of filler either side.
    private val candidateCount = plugin.configYml.getInt("rolls.elimination.candidates")
        .coerceIn(2, 7)

    private val interval = plugin.configYml.getInt("rolls.elimination.interval").coerceAtLeast(1)
    private val revealTime = plugin.configYml.getInt("rolls.elimination.reveal-time")

    private val eliminatedItem = Items.lookup(plugin.configYml.getString("rolls.elimination.eliminated"))

    // The winner is hidden among fillers, and everything except it is knocked out one by one.
    private val candidates = crate.getRandomRewards(player, candidateCount - 1)
        .toMutableList().apply {
            add(reward)
            shuffle()
        }

    private val winnerIndex = candidates.indexOfFirst { it === reward }

    private val slotColumns = List(candidateCount) { ((9 - candidateCount) / 2) + 1 + it }

    private val isEliminated = BooleanArray(candidateCount)

    private var ticksSinceElimination = 0
    private var timeSpentRevealing = 0

    private val gui = menu(3) {
        setMask(
            FillerMask(
                MaskItems(
                    Items.lookup(plugin.configYml.getString("rolls.elimination.filler"))
                ),
                "111111111",
                "111111111",
                "111111111"
            )
        )

        title = crate.name

        for ((index, column) in slotColumns.withIndex()) {
            setSlot(
                2,
                column,
                slot(ItemStack(Material.AIR)) {
                    setUpdater { _, _, _ ->
                        if (isEliminated[index]) {
                            eliminatedItem.item
                        } else {
                            candidates[index].getDisplay(player, crate)
                        }
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
        if (isOnlyWinnerLeft()) {
            timeSpentRevealing++
            return
        }

        ticksSinceElimination++

        if (ticksSinceElimination < interval) {
            return
        }

        ticksSinceElimination = 0

        val victim = candidates.indices
            .filter { it != winnerIndex && !isEliminated[it] }
            .randomOrNull() ?: return

        isEliminated[victim] = true
        gui.refresh(player)

        if (isOnlyWinnerLeft()) {
            player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f)
        } else {
            player.playSound(player.location, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f)
        }
    }

    private fun isOnlyWinnerLeft(): Boolean {
        return isEliminated.count { !it } <= 1
    }

    override fun shouldContinueTicking(tick: Int): Boolean {
        return !isOnlyWinnerLeft() || timeSpentRevealing <= revealTime
    }

    override fun onFinish() {
        player.closeInventory()
    }

    object Factory : RollFactory<RollElimination>("elimination") {
        override fun create(options: RollOptions): RollElimination =
            RollElimination(
                options.reward,
                options.crate,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}
