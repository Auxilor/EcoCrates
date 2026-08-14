package com.willfp.ecocrates.crate.roll

import com.willfp.eco.core.gui.addPage
import com.willfp.eco.core.gui.addPageChanger
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.page.PageChanger
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.sound.PlayableSound
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.crate.isOpeningCrate
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player

class RollChoose private constructor(
    override var reward: Reward,
    override val crate: Crate,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
) : Roll {
    private val revealTime = plugin.configYml.getInt("rolls.choose.reveal-time")

    // If the player never picks, one is chosen for them so the roll can't hang.
    private val autoPickTime = plugin.configYml.getInt("rolls.choose.auto-pick")

    // Every reward the player could actually win, in the same order every time this roll is open.
    private val eligibleRewards = crate.rewards.filter { it.getEffectiveWeight(player) > 0 }

    private var picked = false
    private var timeSpentRevealing = 0

    private val gui = run {
        val pattern = plugin.configYml.getStrings("rolls.choose.mask.pattern")
        val perPage = pattern.sumOf { line -> line.count { it.equals('i', ignoreCase = true) } }
            .coerceAtLeast(1)
        val maxPages = (eligibleRewards.size / perPage + if (eligibleRewards.size % perPage > 0) 1 else 0)
            .coerceAtLeast(1)

        val pageChangeSound = PlayableSound.create(plugin.configYml.getSubsection("rolls.choose.page-change-sound"))

        menu(pattern.size) {
            title = plugin.configYml.getFormattedString("rolls.choose.title")
                .replace("%crate%", crate.name)

            maxPages(maxPages)

            addPageChanger(plugin.configYml, "rolls.choose.forwards-arrow", PageChanger.Direction.FORWARDS, pageChangeSound)
            addPageChanger(plugin.configYml, "rolls.choose.backwards-arrow", PageChanger.Direction.BACKWARDS, pageChangeSound)

            for (pageNum in 1..maxPages) {
                addPage(pageNum) {
                    setMask(
                        FillerMask(
                            MaskItems.fromItemNames(plugin.configYml.getStrings("rolls.choose.mask.items")),
                            *pattern.toTypedArray()
                        )
                    )

                    var rewardIndex = (pageNum - 1) * perPage
                    pattern.forEachIndexed { rowIndex, line ->
                        line.forEachIndexed { colIndex, character ->
                            if (character.equals('i', ignoreCase = true) && rewardIndex < eligibleRewards.size) {
                                val option = eligibleRewards[rewardIndex]

                                setSlot(
                                    rowIndex + 1,
                                    colIndex + 1,
                                    slot(option.getDisplay(player, crate)) {
                                        onLeftClick { _, _, _ ->
                                            pick(option)
                                        }
                                    }
                                )

                                rewardIndex++
                            } else if (character.equals('i', ignoreCase = true)) {
                                rewardIndex++
                            }
                        }
                    }
                }
            }

            onClose { _, _ ->
                player.isOpeningCrate = false
            }
        }
    }

    private fun pick(option: Reward) {
        if (picked) {
            return
        }

        picked = true
        reward = option

        player.playSound(player.location, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f)
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f)
    }

    override fun roll() {
        gui.open(player)
    }

    override fun tick(tick: Int) {
        if (picked) {
            timeSpentRevealing++
            return
        }

        if (tick >= autoPickTime) {
            pick(eligibleRewards.random())
            return
        }

        if (tick % 10 == 0) {
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f, 1.0f)
        }
    }

    override fun shouldContinueTicking(tick: Int): Boolean {
        return !picked || timeSpentRevealing <= revealTime
    }

    override fun onFinish() {
        player.closeInventory()
    }

    object Factory : RollFactory<RollChoose>("choose") {
        override fun create(options: RollOptions): RollChoose =
            RollChoose(
                options.reward,
                options.crate,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}
