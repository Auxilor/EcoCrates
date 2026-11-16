package com.willfp.ecocrates.crate.open

import com.willfp.ecocrates.crate.reroll.MultiReRollGUI
import com.willfp.ecocrates.crate.reroll.ReRollGUI
import com.willfp.ecocrates.crate.roll.ChoiceRoll
import com.willfp.ecocrates.crate.roll.Roll
import com.willfp.ecocrates.crate.roll.RollPickContext
import com.willfp.ecocrates.crate.roll.RollSession
import com.willfp.ecocrates.event.PickInfo
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.PendingRewards
import com.willfp.ecocrates.reward.RewardSource
import com.willfp.ecocrates.reward.pickReward
import org.bukkit.entity.Player
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level

/**
 * One open in progress: every pick decided up front, played through rolls in batches of the
 * roll's `maxRewards`. Rolls that show one reward use the `multi-reward` fallback: `sequential`
 * plays a roll per pick, each with its own reroll offer, and `primary` plays the first pick and
 * gives the bonuses straight after it is accepted. A roll that shows several rewards offers them in
 * [MultiReRollGUI], which returns here after each reroll until the batch is accepted.
 *
 * Once a roll ends, its rewards are copied into the batch's picks before anything is given. The roll
 * first settles any choice left (a choose roll auto-picks them), and the picks of its dropped reward
 * slots leave the open, with the picks left renumbered in order, so the reward amount counts only
 * what is given.
 *
 * Everything here runs on the player's region.
 *
 * @property source The source being opened.
 * @property player The opening player.
 * @param picks Every pick of the open, in order.
 * @property context Where and how the open happens.
 */
internal class OpenSession(
    val source: RewardSource,
    val player: Player,
    picks: List<OpenPick>,
    val context: OpenContext
) {
    private val openPicks = CopyOnWriteArrayList(picks)

    /** Every pick of the open, in order, less any pick a choose roll dropped. */
    val picks: List<OpenPick>
        get() = openPicks

    /** What this open needs from [source]. */
    internal val sessionSource: SessionSource = SessionSource.forSource(source)

    private val batches: List<List<OpenPick>> = splitIntoRollBatches(
        picks = picks,
        maxRewards = sessionSource.maxRewards,
        fallback = source.multiRewardSettings.fallback
    )

    private val batchIndex = AtomicInteger(0)

    private val isFinished = AtomicBoolean(false)

    /** The roll this open is playing now, if any. */
    @Volatile
    var activeRoll: Roll? = null
        private set

    @Volatile
    private var activeBatch: List<OpenPick> = emptyList()

    @Volatile
    private var batchToReopenAfterReroll: List<OpenPick>? = null

    /** The number of rewards in this open, after bonuses that dropped out. */
    val rewardAmount: Int
        get() = picks.size

    /** Starts the first roll. */
    fun play() {
        playBatch(0)
    }

    /**
     * Gives [pick] (or queues it if the player is offline), firing its reward event and finish effects.
     * Runs the all-finish effects once every pick is given.
     */
    fun give(pick: OpenPick) {
        if (!pick.isGiven) {
            pick.isGiven = true
            sessionSource.deliver(player = player, pick = pick, amount = rewardAmount)

            if (picks.all { it.isGiven }) {
                finish()
            }
        }
    }

    /**
     * Gives every pick not yet given, or queues them for the next join.
     *
     * @param queueForLater If the picks are queued even though the player is online.
     */
    fun finishAll(queueForLater: Boolean) {
        activeRoll?.let { adopt(it, activeBatch) }

        val isQueued = queueForLater || !player.isOnline

        try {
            for (pick in picks.filterNot { it.isGiven }) {
                runCatching {
                    if (isQueued) {
                        pick.isGiven = true
                        PendingRewards.queue(player, source, pick.reward)
                    } else {
                        give(pick)
                    }
                }.onFailure { logOpenFailure(it) }
            }
        } finally {
            finish()
        }
    }

    /** Accepts [pick] from its reroll offer, gives it, and plays on. */
    fun accept(pick: OpenPick) {
        if (!pick.isGiven && !isFinished.get()) {
            finishRemainingPicksOnFailure {
                give(pick)
                playBatch(batchIndex.get() + 1)
            }
        }
    }

    /** Accepts every pick of [batch] from [MultiReRollGUI], gives them, and plays on. */
    fun acceptAll(batch: List<OpenPick>) {
        if (batch.any { !it.isGiven } && !isFinished.get()) {
            finishRemainingPicksOnFailure {
                batchToReopenAfterReroll = null
                batch.forEach { give(it) }
                playBatch(batchIndex.get() + 1)
            }
        }
    }

    /**
     * Replaces [pick] with a new pick under the same rules and plays it as a single-reward roll.
     *
     * @param pick The pick to reroll.
     * @param multiBatch The batch [MultiReRollGUI] showed, to open it again once the reroll finishes,
     * or null to offer the single-pick menu as usual.
     * @return If the reroll started.
     */
    fun reroll(pick: OpenPick, multiBatch: List<OpenPick>? = null): Boolean = runCatching {
        val replacement = if (
            !isFinished.get()
            && !pick.isGiven
            && sessionSource.canStartReroll(player)
        ) {
            val others = heldOutsideBatch(picks = picks, batch = listOf(pick), isReroll = true)

            source.pickReward(
                player = player,
                taken = others.map { it.reward },
                isBonus = pick.isBonus,
                isReroll = true,
                pending = others.filterNot { it.isGiven }.map { it.reward }
            )?.takeIf { sessionSource.canReroll(player, pick) }
        } else {
            null
        }

        val reward = replacement?.let {
            sessionSource.beginReroll(player, context.method, it, PickInfo(pick.number, pick.isBonus))
        }

        if (reward != null) {
            pick.reward = reward
            pick.rerolls++
            batchToReopenAfterReroll = multiBatch
            startRoll(listOf(pick), isReroll = true)
        }

        reward != null
    }.getOrElse { error ->
        logOpenFailure(error)
        finishAll(false)
        false
    }

    private fun playBatch(index: Int) {
        batchIndex.set(index)
        val batch = batches.getOrNull(index)

        if (batch == null || isFinished.get() || !player.isOnline) {
            finishAll(false)
        } else {
            startRoll(batch, isReroll = false)
        }
    }

    private fun startRoll(batch: List<OpenPick>, isReroll: Boolean) {
        val others = heldOutsideBatch(picks = picks, batch = batch, isReroll = isReroll)
        val roll = sessionSource.createRoll(
            player = player,
            context = context,
            rewards = batch.map { it.reward },
            isReroll = isReroll,
            pickContext = RollPickContext(
                bonusSlots = batch.map { it.isBonus },
                otherPicks = others.map { it.reward },
                pendingPicks = others.filterNot { it.isGiven }.map { it.reward }
            )
        )

        activeBatch = batch
        activeRoll = roll

        sessionSource.hidePlacedCrate(player, context)

        RollSession.start(roll) { finishedRoll, forced -> onRolled(batch, finishedRoll, forced) }
    }

    private fun onRolled(batch: List<OpenPick>, roll: Roll, forced: Boolean) = finishRemainingPicksOnFailure {
        val kept = adopt(roll, batch)
        sessionSource.showPlacedCrate(player, context)

        val pick = kept.singleOrNull()
        val multiBatch = batchToReopenAfterReroll?.filter { openPicks.holds(it) } ?: kept.takeIf { it.size > 1 }

        when {
            forced || !player.isOnline -> {
                finishAll(false)
            }

            multiBatch != null -> {
                MultiReRollGUI.open(this, multiBatch)
            }

            pick != null && sessionSource.canReroll(player, pick) -> {
                ReRollGUI.open(this, pick)
            }

            else -> {
                kept.forEach { give(it) }
                playBatch(batchIndex.get() + 1)
            }
        }
    }

    private fun adopt(roll: Roll, batch: List<OpenPick>): List<OpenPick> {
        activeRoll = null
        (roll as? ChoiceRoll)?.completePendingChoices()

        if (batch.size == 1) {
            batch.single().reward = roll.reward
        } else {
            roll.rewards.zip(batch).forEach { (reward, pick) -> pick.reward = reward }
        }

        (roll as? ChoiceRoll)?.droppedRewardSlots.orEmpty()
            .mapNotNull { batch.getOrNull(it) }
            .forEach { dropped -> openPicks.removeIf { it === dropped } }
        openPicks.forEachIndexed { index, pick -> pick.number = index + 1 }

        return batch.filter { openPicks.holds(it) }
    }

    private fun finish() {
        if (isFinished.compareAndSet(false, true)) {
            OpenSessions.unregister(player)

            if (player.isOnline) {
                runCatching { sessionSource.finishOpen(player, picks.map { it.reward }) }
                    .onFailure { logOpenFailure(it) }
            }
        }
    }
}

private fun List<OpenPick>.holds(pick: OpenPick): Boolean = any { it === pick }

private fun OpenSession.logOpenFailure(error: Throwable) {
    plugin.logger.log(Level.WARNING, "Error during a ${source.sourceType} open for ${player.name}", error)
}

private inline fun OpenSession.finishRemainingPicksOnFailure(block: () -> Unit) {
    runCatching { block() }.onFailure { error ->
        logOpenFailure(error)
        finishAll(false)
    }
}
