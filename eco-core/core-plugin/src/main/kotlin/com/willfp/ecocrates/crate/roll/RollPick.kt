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

class RollPick private constructor(
    override val reward: Reward,
    override val crate: Crate,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
) : Roll {
    private val boxCount = plugin.configYml.getInt("rolls.pick.boxes").coerceIn(2, 7)
    private val revealTime = plugin.configYml.getInt("rolls.pick.reveal-time")

    // How long the picked box is shown on its own before the other boxes open.
    private val soloRevealTime = plugin.configYml.getInt("rolls.pick.solo-reveal-time")

    // If the player never picks, one is chosen for them so the roll can't hang.
    private val autoPickTime = plugin.configYml.getInt("rolls.pick.auto-pick")

    private val boxItem = Items.lookup(plugin.configYml.getString("rolls.pick.box"))

    // What the boxes the player didn't choose turn out to have been holding.
    private val nearMisses = crate.getRandomRewards(player, boxCount)

    private val slotColumns = List(boxCount) { ((9 - boxCount) / 2) + 1 + it }

    private var pickedIndex: Int? = null
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
            .replace("%crate%", crate.name)

        for ((index, column) in slotColumns.withIndex()) {
            setSlot(
                2,
                column,
                slot(ItemStack(Material.AIR)) {
                    setUpdater { _, _, _ ->
                        when {
                            pickedIndex == null -> boxItem.item
                            pickedIndex == index -> reward.getDisplay(player, crate)
                            !hasOpenedOthers -> boxItem.item
                            else -> nearMisses[index].getDisplay(player, crate)
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

    private fun pick(index: Int) {
        if (pickedIndex != null) {
            return
        }

        pickedIndex = index
        gui.refresh(player)

        player.playSound(player.location, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f)
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f)
    }

    override fun roll() {
        gui.open(player)
    }

    override fun tick(tick: Int) {
        if (pickedIndex != null) {
            timeSpentRevealing++

            if (!hasOpenedOthers && timeSpentRevealing >= soloRevealTime) {
                hasOpenedOthers = true
                gui.refresh(player)

                player.playSound(player.location, Sound.BLOCK_CHEST_OPEN, 0.7f, 0.8f)
            }

            return
        }

        if (tick >= autoPickTime) {
            pick(slotColumns.indices.random())
            return
        }

        if (tick % 10 == 0) {
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f, 1.0f)
        }
    }

    override fun shouldContinueTicking(tick: Int): Boolean {
        return pickedIndex == null || timeSpentRevealing <= soloRevealTime + revealTime
    }

    override fun onFinish() {
        player.closeInventory()
    }

    object Factory : RollFactory<RollPick>("pick") {
        override fun create(options: RollOptions): RollPick =
            RollPick(
                options.reward,
                options.crate,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}
