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
 * A scratchcard: the player scratches cards off until they turn up to-match copies
 * of the same reward, and that reward is what they win.
 *
 * The card is dealt when the roll starts. to-match slots hold each winning reward, one
 * set per reward, and a losing reward never gets more than to-match - 1 copies, so it
 * can tease a set but never complete one. However the player scratches, the only sets
 * on the card are the rewards the crate already picked for them. With several rewards
 * the player scratches until every set is found, and the card grows a row if it needs
 * one to fit the extra sets.
 *
 * The only thing that isn't left to chance is an instant win: completing a set
 * before min-scratches moves the winner under a card that hasn't been scratched
 * yet. The player can't see unscratched cards, so it stays hidden, and past the
 * floor the card behaves exactly as it was dealt.
 */
class RollMatch private constructor(
    override val rewards: List<Reward>,
    override val source: RewardSource,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
) : Roll {
    override val reward: Reward = rewards.first()

    private val maxCardRows = ChoiceRolls.MATCH_MAX_ROWS
    private val maxCardsPerRow = ChoiceRolls.MATCH_CARDS_PER_ROW

    private val toMatch = ChoiceRolls.matchToMatch(plugin.configYml.getInt("rolls.match.to-match"))

    private val fillerRewards = source.rewards
        .asSequence()
        .filter { it !in rewards }
        .distinct()
        .shuffled()
        .toList()

    /*
    A losing reward may appear up to to-match - 1 times: enough to tease a set the
    player can't complete, never enough to be a second winner. With to-match at 2
    that means every loser is unique, since any repeat would be a winning pair.

    That also decides the card size, which is why there's nothing to configure: the
    card is dealt as large as the crate's own rewards can fill, up to the grid. The
    losers keep the room they have on a single-reward card; extra sets add cards.
     */
    private val maxCopiesPerFiller = toMatch - 1

    private val losingCards = run {
        val capacity = ChoiceRolls.matchLoserCapacity(toMatch = toMatch, rewards = rewards.size)
        val copies = fillerRewards.associateWithTo(mutableMapOf()) { 0 }
        val dealt = mutableListOf<Reward>()

        while (dealt.size < capacity) {
            val eligible = copies.filterValues { it < maxCopiesPerFiller }.keys

            val next = eligible.randomOrNull() ?: break

            copies[next] = copies.getValue(next) + 1
            dealt.add(next)
        }

        dealt
    }

    private val cardCount = losingCards.size + rewards.size * toMatch

    private val revealTime = plugin.configYml.getInt("rolls.match.reveal-time")

    // If the player stops scratching, cards are scratched for them so the roll ends.
    private val autoScratchTime = plugin.configYml.getInt("rolls.match.auto-scratch").coerceAtLeast(1)

    // The set can't complete before this many scratches. Never more than the card
    // can hold, or the winner would have nowhere left to move to.
    private val minScratches = plugin.configYml.getInt("rolls.match.min-scratches")
        .coerceIn(toMatch, cardCount)

    private val cardItem = Items.lookup(plugin.configYml.getString("rolls.match.card"))

    // Cards are centred in the menu, in rows of at most maxCardsPerRow.
    private val cardRows = ((cardCount + maxCardsPerRow - 1) / maxCardsPerRow).coerceIn(1, maxCardRows)

    private val slotsInUse = run {
        val perRow = List(cardRows) { row ->
            cardCount / cardRows + if (row < cardCount % cardRows) 1 else 0
        }

        perRow.flatMapIndexed { row, count ->
            List(count) { position -> (row + 2) to (((9 - count) / 2) + 1 + position) }
        }
    }

    private val isScratched = BooleanArray(cardCount)

    // The slots holding the winners, to-match per reward; the rest are dealt a filler each.
    private val winningSlots = (0 until cardCount).shuffled().take(rewards.size * toMatch).toMutableSet()

    private val card = run {
        val losers = losingCards.iterator()
        val winners = rewards.flatMap { winner -> List(toMatch) { winner } }.iterator()

        Array(cardCount) { index ->
            if (index in winningSlots) winners.next() else losers.next()
        }
    }

    private var scratchCount = 0
    private var timeSpentRevealing = 0
    private var ticksSinceScratch = 0

    private val gui = menu(cardRows + 2) {
        setMask(
            FillerMask(
                MaskItems(
                    Items.lookup(plugin.configYml.getString("rolls.match.filler"))
                ),
                *Array(cardRows + 2) { "111111111" }
            )
        )

        title = plugin.configYml.getFormattedString("rolls.match.title")
            .replace("%crate%", source.name)

        for ((index, position) in slotsInUse.withIndex()) {
            val (row, column) = position

            setSlot(
                row,
                column,
                slot(ItemStack(Material.AIR)) {
                    setUpdater { _, _, _ ->
                        if (isScratched[index]) {
                            card[index].getDisplay(player, source)
                        } else {
                            cardItem.item
                        }
                    }

                    onLeftClick { _, _, _ ->
                        scratch(index)
                    }
                }
            )
        }

        onClose { _, _ ->
            player.isOpeningCrate = false
        }
    }

    private fun scratch(index: Int) {
        if (hasMatched() || isScratched[index]) {
            return
        }

        if (isTooEarlyToWin(index)) {
            moveWinnerElsewhere(index)
        }

        isScratched[index] = true
        scratchCount++
        ticksSinceScratch = 0

        when {
            hasMatched() -> {
                // Show what was under the rest, so the near misses are visible.
                isScratched.fill(true)

                player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f)
            }

            index !in winningSlots -> {
                player.playSound(player.location, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0f, 1.0f)
            }

            setProgressAt(index) == toMatch -> {
                player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f)
            }

            else -> {
                // Part of the set: the pitch climbs with every one found.
                player.playSound(
                    player.location,
                    Sound.BLOCK_NOTE_BLOCK_BELL,
                    1.0f,
                    (0.8 + 0.6 * setProgressAt(index) / toMatch).toFloat()
                )
            }
        }

        gui.refresh(player)
    }

    private fun setProgressAt(index: Int): Int =
        ChoiceRolls.setProgress(found = scratchedCopiesOf(card[index]), toMatch = toMatch)

    private fun scratchedCopiesOf(winner: Reward): Int =
        winningSlots.count { isScratched[it] && card[it] == winner }

    /**
     * Whether scratching [index] would complete a set sooner than the floor allows.
     */
    private fun isTooEarlyToWin(index: Int): Boolean {
        if (scratchCount + 1 >= minScratches) {
            return false
        }

        return index in winningSlots &&
            ChoiceRolls.setProgress(found = scratchedCopiesOf(card[index]) + 1, toMatch = toMatch) == toMatch
    }

    /**
     * Swap the winning card out of [index] and into a card the player hasn't scratched,
     * so the pair lands later. Silent - unscratched cards give nothing away.
     */
    private fun moveWinnerElsewhere(index: Int) {
        val destination = isScratched.indices
            .filter { it != index && !isScratched[it] && it !in winningSlots }
            .randomOrNull() ?: return

        // Straight swap, so the cards are only ever rearranged - every reward keeps
        // the number of copies it was dealt, and no loser can grow into a set.
        card[index] = card[destination].also { card[destination] = card[index] }

        winningSlots.remove(index)
        winningSlots.add(destination)
    }

    private fun hasMatched(): Boolean {
        return winningSlots.all { isScratched[it] }
    }

    override fun roll() {
        gui.open(player)
    }

    override fun tick(tick: Int) {
        if (hasMatched()) {
            timeSpentRevealing++
            return
        }

        ticksSinceScratch++

        if (ticksSinceScratch >= autoScratchTime) {
            isScratched.indices
                .filter { !isScratched[it] }
                .randomOrNull()
                ?.let { scratch(it) }
        }
    }

    override fun shouldContinueTicking(tick: Int): Boolean {
        return !hasMatched() || timeSpentRevealing <= revealTime
    }

    override fun onFinish() {
        player.closeInventory()
    }

    object Factory : RollFactory<RollMatch>("match") {
        override val isGuiRoll = true

        override val maxRewards: Int
            get() = ChoiceRolls.matchCapacity(ChoiceRolls.matchToMatch(plugin.configYml.getInt("rolls.match.to-match")))

        override fun create(options: RollOptions): RollMatch =
            RollMatch(
                options.rewards,
                options.source,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}
