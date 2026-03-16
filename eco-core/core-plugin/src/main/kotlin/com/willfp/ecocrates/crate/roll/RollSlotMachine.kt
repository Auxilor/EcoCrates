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

class RollSlotMachine private constructor(
    override val reward: Reward,
    override val crate: Crate,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
) : Roll {
    private val reelRows = listOf(1, 2, 3)
    private val middleRow = 2

    private val symbolCount = plugin.configYml.getInt("rolls.slot_machine.symbols")
    private val spinTicks = plugin.configYml.getInt("rolls.slot_machine.spin-ticks")
    private val reelStopDelay = plugin.configYml.getInt("rolls.slot_machine.reel-stop-delay")
    private val spinInterval = plugin.configYml.getInt("rolls.slot_machine.spin-interval")
    private val startPitch = plugin.configYml.getDouble("rolls.slot_machine.start-pitch")
    private val pitchStep = plugin.configYml.getDouble("rolls.slot_machine.pitch-step")

    // Fixed five-column slot reel layout.
    private val reelColumns = listOf(3, 4, 5, 6, 7)

    private val reelStrips = reelColumns.map {
        crate.getRandomRewards(player, symbolCount)
    }

    private val offsets = IntArray(reelColumns.size)
    private val reelStopped = BooleanArray(reelColumns.size)
    private var ticksElapsed = 0
    private var ticksSinceSpin = 0

    private val gui = menu(3) {
        setMask(
            FillerMask(
                MaskItems(
                    Items.lookup(plugin.configYml.getString("rolls.slot_machine.filler")),
                    Items.lookup(plugin.configYml.getString("rolls.slot_machine.selector"))
                ),
                "110000011",
                "110000011",
                "110000011"
            )
        )

        title = crate.name

        for ((index, column) in reelColumns.withIndex()) {
            for (row in reelRows) {
                setSlot(
                    row,
                    column,
                    slot(ItemStack(Material.AIR)) {
                        setUpdater { _, _, _ ->
                            if (reelStopped[index] && row == middleRow) {
                                reward.getDisplay(player, crate)
                            } else {
                                val strip = reelStrips[index]
                                val rowOffset = row - middleRow
                                strip[(offsets[index] + rowOffset).mod(strip.size)].getDisplay(player, crate)
                            }
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
        ticksElapsed++
        ticksSinceSpin++

        if (ticksSinceSpin >= spinInterval) {
            ticksSinceSpin = 0

            for (index in reelColumns.indices) {
                if (reelStopped[index]) {
                    continue
                }

                offsets[index]++

                val reelStopTick = spinTicks + (index * reelStopDelay)
                if (ticksElapsed >= reelStopTick) {
                    reelStopped[index] = true
                }
            }

            gui.refresh(player)

            val stoppedCount = reelStopped.count { it }
            val pitch = (startPitch + (pitchStep * stoppedCount)).toFloat()
            player.playSound(player.location, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 0.9f, pitch)
        }
    }

    override fun shouldContinueTicking(tick: Int): Boolean {
        return reelStopped.any { !it }
    }

    override fun onFinish() {
        player.closeInventory()
    }

    object Factory : RollFactory<RollSlotMachine>("slot_machine") {
        override fun create(options: RollOptions): RollSlotMachine =
            RollSlotMachine(
                options.reward,
                options.crate,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}


