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
 * A row of boxes: the player picks one box per reward, and each picked box reveals the next reward
 * in the order picked. The boxes the player didn't pick then open to show near misses.
 */
class RollPick private constructor(
    override val rewards: List<Reward>,
    override val source: RewardSource,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
) : Roll {
    override val reward: Reward = rewards.first()

    private val boxCount = ChoiceRolls.pickBoxes(
        configured = plugin.configYml.getInt("rolls.pick.boxes"),
        rewards = rewards.size
    )

    private val revealTime = plugin.configYml.getInt("rolls.pick.reveal-time")

    // How long the picked boxes are shown on their own before the other boxes open.
    private val soloRevealTime = plugin.configYml.getInt("rolls.pick.solo-reveal-time")

    // If the player doesn't pick every box, the rest are chosen for them so the roll can't hang.
    private val autoPickTime = plugin.configYml.getInt("rolls.pick.auto-pick")

    private val boxItem = Items.lookup(plugin.configYml.getString("rolls.pick.box"))

    // What the boxes the player didn't choose turn out to have been holding.
    private val nearMisses = source.getRandomRewards(player, boxCount)

    private val slotColumns = List(boxCount) { ((9 - boxCount) / 2) + 1 + it }

    private val revealedRewardIndex = IntArray(boxCount) { UNPICKED }
    private var picksMade = 0
    private var timeSpentRevealing = 0
    private var hasOpenedOthers = false

    private val gui = menu(3) {
        setMask(
            FillerMask(
                MaskItems(
                    Items.lookup(plugin.configYml.getString("rolls.pick.filler"))
                ),
                "111111111",
                "111111111",
                "111111111"
            )
        )

        title = plugin.configYml.getFormattedString("rolls.pick.title")
            .replace("%crate%", source.name)

        for ((index, column) in slotColumns.withIndex()) {
            setSlot(
                2,
                column,
                slot(ItemStack(Material.AIR)) {
                    setUpdater { _, _, _ ->
                        when {
                            revealedRewardIndex[index] != UNPICKED ->
                                rewards[revealedRewardIndex[index]].getDisplay(player, source)
                            !hasOpenedOthers -> boxItem.item
                            else -> nearMisses[index].getDisplay(player, source)
                        }
                    }

                    onLeftClick { _, _, _ ->
                        pick(index)
                    }
                }
            )
        }

        onClose { _, _ ->
            player.isOpeningCrate = false
        }
    }

    private fun hasPickedAll(): Boolean = picksMade >= rewards.size

    private fun pick(index: Int) {
        if (hasPickedAll() || revealedRewardIndex[index] != UNPICKED) {
            return
        }

        revealedRewardIndex[index] = picksMade
        picksMade++
        gui.refresh(player)

        player.playSound(player.location, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f)
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f)
    }

    override fun roll() {
        gui.open(player)
    }

    override fun tick(tick: Int) {
        if (hasPickedAll()) {
            timeSpentRevealing++

            if (!hasOpenedOthers && timeSpentRevealing >= soloRevealTime) {
                hasOpenedOthers = true
                gui.refresh(player)

                player.playSound(player.location, Sound.BLOCK_CHEST_OPEN, 0.7f, 0.8f)
            }

            return
        }

        if (tick >= autoPickTime) {
            while (!hasPickedAll()) {
                pick(slotColumns.indices.filter { revealedRewardIndex[it] == UNPICKED }.random())
            }
            return
        }

        if (tick % 10 == 0) {
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f, 1.0f)
        }
    }

    override fun shouldContinueTicking(tick: Int): Boolean =
        !hasPickedAll() || timeSpentRevealing <= soloRevealTime + revealTime

    override fun onFinish() {
        player.closeInventory()
    }

    private companion object {
        const val UNPICKED = -1
    }

    object Factory : RollFactory<RollPick>("pick") {
        override val isGuiRoll = true

        override val maxRewards = ChoiceRolls.PICK_MAX_REWARDS

        override fun create(options: RollOptions): RollPick =
            RollPick(
                options.rewards,
                options.source,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}
