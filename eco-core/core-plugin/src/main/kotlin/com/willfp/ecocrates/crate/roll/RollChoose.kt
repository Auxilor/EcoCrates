package com.willfp.ecocrates.crate.roll

import com.willfp.eco.core.gui.addPage
import com.willfp.eco.core.gui.addPageChanger
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.page.PageChanger
import com.willfp.eco.core.gui.setMask
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.Slot
import com.willfp.eco.core.sound.PlayableSound
import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.crate.isOpeningCrate
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.RewardSource
import com.willfp.ecocrates.reward.pickRule
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Lets the player choose their reward instead of the crate picking one at random, for crates that
 * must not be random. The player makes one choice per reward in the same menu, each replacing
 * the matching entry of [rewards].
 *
 * The first choice of an open shows every reward the player could win. On a reroll of the first
 * choice, any reward the other picks of the open already hold max-per-open times is hidden, unless
 * that would leave nothing to show. A bonus choice hides the rewards the open's pick rules rule out:
 * bonus-eligible false, and any reward already held max-per-open times by the other picks of the open
 * and the earlier choices. A bonus choice with
 * nothing left to show is dropped, and the open session removes its pick before anything is given.
 * With several choices, choices left when the roll is forced to finish or the menu is closed are made the
 * same way as auto-pick, without sounds. With one choice nothing is made then, so the planned reward is given,
 * as it always has been.
 *
 * With one choice the options never change, so each option slot is fixed and positions past the last
 * option stay empty, as they always have. With several choices each option slot shows whatever the
 * current choice offers there, and the pages are sized for every reward the player could win.
 */
class RollChoose private constructor(
    private val options: RollOptions
) : Roll, ChoiceRoll {
    override val source: RewardSource
        get() = options.source

    override val player: Player
        get() = options.player

    override val location: Location
        get() = options.location

    override val isReroll: Boolean
        get() = options.isReroll

    override val method: OpenMethod
        get() = options.method

    private val revealTime = plugin.configYml.getInt("rolls.choose.reveal-time")

    // If the player doesn't make every choice in time, the rest are chosen for them so the roll can't hang.
    private val autoPickTime = plugin.configYml.getInt("rolls.choose.auto-pick")

    // Every reward the player could actually win, in the same order every time this roll is open.
    private val eligibleRewards = source.rewards.filter { it.getEffectiveWeight(player) > 0 }
        .ifEmpty { listOf(options.rewards.first()) }

    private val choices = ChoiceSequence(
        planned = options.rewards,
        isBonus = { options.pickContext.isBonus(it) },
        candidates = eligibleRewards,
        heldElsewhere = options.pickContext.otherPicks,
        rule = pickRule(player),
        pendingElsewhere = options.pickContext.pendingPicks
    )

    override var reward: Reward
        get() = choices.choices.first()
        set(value) {
            choices.replace(0, value)
        }

    override val rewards: List<Reward>
        get() = choices.choices

    override val droppedRewardSlots: Set<Int>
        get() = choices.droppedSlots

    private var timeSpentRevealing = 0

    private val gui = run {
        val pattern = plugin.configYml.getStrings("rolls.choose.mask.pattern")
        val perPage = pattern.sumOf { line -> line.count { it.equals('i', ignoreCase = true) } }
            .coerceAtLeast(1)
        val pagedRewards = if (choices.size == 1) choices.shown else eligibleRewards
        val maxPages = (pagedRewards.size / perPage + if (pagedRewards.size % perPage > 0) 1 else 0)
            .coerceAtLeast(1)

        val pageChangeSound = PlayableSound.create(plugin.configYml.getSubsection("rolls.choose.page-change-sound"))

        menu(pattern.size) {
            title = plugin.configYml.getFormattedString("rolls.choose.title")
                .replace("%crate%", source.name)

            maxPages(maxPages)

            addPageChanger(plugin.configYml, "rolls.choose.forwards-arrow", PageChanger.Direction.FORWARDS, pageChangeSound)
            addPageChanger(plugin.configYml, "rolls.choose.backwards-arrow", PageChanger.Direction.BACKWARDS, pageChangeSound)

            for (pageNum in 1..maxPages) {
                addPage(pageNum) {
                    setMask(plugin.configYml, "rolls.choose")

                    var rewardIndex = (pageNum - 1) * perPage
                    pattern.forEachIndexed { rowIndex, line ->
                        line.forEachIndexed { colIndex, character ->
                            if (character.equals('i', ignoreCase = true)) {
                                optionSlot(rewardIndex)?.let { setSlot(rowIndex + 1, colIndex + 1, it) }

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

    private fun optionSlot(rewardIndex: Int): Slot? = if (choices.size == 1) {
        choices.shown.getOrNull(rewardIndex)?.let { option ->
            slot(option.getDisplay(player, source)) {
                onLeftClick { _, _, _ ->
                    pick(option)
                }
            }
        }
    } else {
        slot(ItemStack(Material.AIR)) {
            setUpdater { _, _, _ ->
                choices.shown.getOrNull(rewardIndex)?.getDisplay(player, source) ?: ItemStack(Material.AIR)
            }

            onLeftClick { _, _, menu ->
                choices.shown.getOrNull(rewardIndex)?.let { pick(it) }
                menu.refresh(player)
            }
        }
    }

    override fun completePendingChoices() {
        if (choices.size > 1) {
            choices.completeAtRandom()
        }
    }

    private fun pick(option: Reward) {
        if (choices.isComplete()) {
            return
        }

        choices.choose(option)

        player.playSound(player.location, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f)
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f)
    }

    override fun roll() {
        gui.open(player)
    }

    override fun tick(tick: Int) {
        if (choices.isComplete()) {
            timeSpentRevealing++
            return
        }

        if (tick >= autoPickTime) {
            while (!choices.isComplete()) {
                pick(choices.shown.random())
            }
            return
        }

        if (tick % 10 == 0) {
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f, 1.0f)
        }
    }

    override fun shouldContinueTicking(tick: Int): Boolean =
        !choices.isComplete() || timeSpentRevealing <= revealTime

    override fun onFinish() {
        player.closeInventory()
    }

    object Factory : RollFactory<RollChoose>("choose") {
        override val isGuiRoll = true

        /** Unlimited: each choice offers whatever the open's pick rules still allow. */
        override val maxRewards = Int.MAX_VALUE

        override fun create(options: RollOptions): RollChoose =
            RollChoose(options)
    }
}
