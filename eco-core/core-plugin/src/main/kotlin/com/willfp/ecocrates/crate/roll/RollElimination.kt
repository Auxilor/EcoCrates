package com.willfp.ecocrates.crate.roll

import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.items.Items
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
 * Shows a row of candidates and knocks them out one by one until only the winners remain.
 * With several rewards, each remaining candidate shows its own reward.
 */
class RollElimination private constructor(
    override val rewards: List<Reward>,
    override val source: RewardSource,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
) : Roll {
    override val reward: Reward = rewards.first()

    // Seven is the widest row that still leaves a border of filler either side.
    private val candidateCount = GuiRollLayout.eliminationCandidates(
        configured = plugin.configYml.getInt("rolls.elimination.candidates"),
        rewards = rewards.size
    )

    private val interval = plugin.configYml.getInt("rolls.elimination.interval").coerceAtLeast(1)
    private val revealTime = plugin.configYml.getInt("rolls.elimination.reveal-time")

    private val eliminatedItem = Items.lookup(plugin.configYml.getString("rolls.elimination.eliminated"))

    // The winners are hidden among fillers, and everything except them is knocked out one by one.
    private val order = RingItems.order(itemCount = candidateCount, rewards = rewards.size)

    private val candidates = source.getRandomRewards(player, candidateCount - rewards.size)
        .iterator()
        .let { fillers -> order.map { if (it == RingItems.FILLER) fillers.next() else rewards[it] } }

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

        title = source.name

        for ((index, column) in slotColumns.withIndex()) {
            setSlot(
                2,
                column,
                slot(ItemStack(Material.AIR)) {
                    setUpdater { _, _, _ ->
                        if (isEliminated[index]) {
                            eliminatedItem.item
                        } else {
                            candidates[index].getDisplay(player, source)
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
        if (areOnlyWinnersLeft()) {
            timeSpentRevealing++
            return
        }

        ticksSinceElimination++

        if (ticksSinceElimination < interval) {
            return
        }

        ticksSinceElimination = 0

        val victim = candidates.indices
            .filter { order[it] == RingItems.FILLER && !isEliminated[it] }
            .randomOrNull()
            ?: return

        isEliminated[victim] = true
        gui.refresh(player)

        if (areOnlyWinnersLeft()) {
            player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f)
        } else {
            player.playSound(player.location, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f)
        }
    }

    private fun areOnlyWinnersLeft(): Boolean = isEliminated.count { !it } <= rewards.size

    override fun shouldContinueTicking(tick: Int): Boolean = !areOnlyWinnersLeft() || timeSpentRevealing <= revealTime

    override fun onFinish() {
        player.closeInventory()
    }

    object Factory : RollFactory<RollElimination>("elimination") {
        override val isGuiRoll = true

        override val maxRewards: Int
            get() = GuiRollLayout.eliminationCapacity(plugin.configYml.getInt("rolls.elimination.candidates"))

        override fun create(options: RollOptions): RollElimination =
            RollElimination(
                options.rewards,
                options.source,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}
