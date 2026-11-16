package com.willfp.ecocrates.crate.roll

import com.willfp.ecocrates.reward.PickRule
import com.willfp.ecocrates.reward.PickRules
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.random.Random

/**
 * The choices of one choose roll, made in order, one per reward of the roll.
 *
 * Each pick shows only the candidates the open's pick rules still allow, counting the rewards held
 * elsewhere in the open and the earlier choices. The main pick falls back to every candidate when
 * the rules leave none.
 * A bonus pick also hides the candidates that are not bonus-eligible.
 * A bonus pick with nothing left to show is dropped: it is skipped, and its reward is neither
 * given nor counted as held.
 *
 * @param T The candidate type.
 * @param planned The rewards the open planned, one per choice. Each choice replaces its entry.
 * @param isBonus If the choice at an index is a bonus pick.
 * @param candidates Every option the roll can show, in display order.
 * @param heldElsewhere The rewards the open's other picks already hold.
 * @param rule The open's pick rules.
 * @param pendingElsewhere The rewards of [heldElsewhere] not yet given.
 */
class ChoiceSequence<T>(
    planned: List<T>,
    private val isBonus: (Int) -> Boolean,
    private val candidates: List<T>,
    private val heldElsewhere: List<T>,
    private val rule: PickRule<T>,
    private val pendingElsewhere: List<T> = heldElsewhere
) {
    private val chosen = CopyOnWriteArrayList(planned)
    private val dropped = CopyOnWriteArraySet<Int>()

    /** The number of choices made or dropped so far. */
    var choicesMade = 0
        private set

    /** The options of the current choice, never empty while a choice is left. */
    var shown: List<T> = emptyList()
        private set

    /** One entry per choice: the chosen reward, or the planned one where no choice is made yet or it is dropped. */
    val choices: List<T>
        get() = chosen.toList()

    /** The indices of the dropped choices. */
    val droppedSlots: Set<Int>
        get() = dropped.toSet()

    /** The number of choices, made or not. */
    val size: Int
        get() = chosen.size

    init {
        skipChoicesWithNoOptions()
    }

    /**
     * Gets if every choice is made or dropped.
     *
     * @return If no choice is left.
     */
    fun isComplete(): Boolean = choicesMade >= chosen.size

    /**
     * Replaces the reward at a choice, without making the choice.
     *
     * @param slot The index of the choice.
     * @param value The new reward.
     */
    fun replace(slot: Int, value: T) {
        chosen[slot] = value
    }

    /**
     * Makes the current choice, then moves to the next choice that has options.
     *
     * @param option The chosen reward.
     */
    fun choose(option: T) {
        if (!isComplete()) {
            chosen[choicesMade] = option
            choicesMade++
            skipChoicesWithNoOptions()
        }
    }

    /**
     * Makes every choice left at random from what each one shows, in order.
     *
     * @param random The source of randomness.
     */
    fun completeAtRandom(random: Random = Random.Default) {
        while (!isComplete()) {
            choose(shown.random(random))
        }
    }

    private fun optionsForChoice(choice: Int): List<T> {
        val earlierChoices = chosen.take(choice).filterIndexed { index, _ -> index !in dropped }
        val eligible = PickRules.eligible(
            candidates = candidates,
            taken = heldElsewhere + earlierChoices,
            isBonus = isBonus(choice),
            rule = rule,
            pending = pendingElsewhere + earlierChoices
        )

        return if (isBonus(choice)) eligible else eligible.ifEmpty { candidates }
    }

    private fun skipChoicesWithNoOptions() {
        while (!isComplete() && optionsForChoice(choicesMade).isEmpty()) {
            dropped.add(choicesMade)
            choicesMade++
        }

        if (!isComplete()) {
            shown = optionsForChoice(choicesMade)
        }
    }
}
