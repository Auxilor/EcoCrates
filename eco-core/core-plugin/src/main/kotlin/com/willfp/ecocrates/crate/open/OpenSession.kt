package com.willfp.ecocrates.crate.open

import com.willfp.eco.core.price.ConfiguredPrice
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.reroll.ReRollGUI
import com.willfp.ecocrates.crate.roll.Roll
import com.willfp.ecocrates.crate.roll.RollSession
import com.willfp.ecocrates.event.PickInfo
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.PendingRewards
import com.willfp.ecocrates.reward.RewardSource
import com.willfp.ecocrates.reward.pickReward
import org.bukkit.entity.Player
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level

/**
 * One open in progress: every pick decided up front, played through rolls in batches of the
 * roll's `maxRewards`. Rolls that show one reward use the `multi-reward` fallback: `sequential`
 * plays a roll per pick, each with its own reroll offer, and `primary` plays the first pick and
 * gives the bonuses straight after it is accepted.
 *
 * Everything here runs on the player's region.
 *
 * @property source The source being opened.
 * @property player The opening player.
 * @property picks Every pick of the open, in order.
 * @property context Where and how the open happens.
 */
class OpenSession(
    val source: RewardSource,
    val player: Player,
    val picks: List<OpenPick>,
    val context: OpenContext
) {
    private val crate: Crate = requireNotNull(source as? Crate) { "Only crates open through sessions" }

    private val batches: List<List<OpenPick>> = splitIntoRollBatches(
        picks = picks,
        maxRewards = crate.rollFactory.maxRewards,
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
            crate.deliver(player = player, pick = pick, amount = rewardAmount)

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
                }.onFailure { logOpenFailure(player, it) }
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

    /** The price of the next reroll of [pick]. */
    fun rerollPrice(pick: OpenPick): ConfiguredPrice = crate.rerollPrice(pick)

    /**
     * Replaces [pick] with a new pick under the same rules and plays it as a single-reward roll.
     *
     * @return If the reroll started.
     */
    fun reroll(pick: OpenPick): Boolean = runCatching {
        val replacement = if (
            !isFinished.get()
            && !pick.isGiven
            && crate.canStartReroll(player)
        ) {
            source.pickReward(
                player = player,
                taken = picks.filter { it !== pick }.map { it.reward },
                isBonus = pick.isBonus
            )
        } else {
            null
        }

        val reward = replacement?.let {
            crate.fireOpenEvent(player, context.method, it, true, PickInfo(pick.number, pick.isBonus))
        }

        if (reward != null) {
            pick.reward = reward
            pick.rerolls++
            crate.recordOpen(player)
            startRoll(listOf(pick), isReroll = true)
        }

        reward != null
    }.getOrElse { error ->
        logOpenFailure(player, error)
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
        val roll = crate.createRoll(player, context, batch.map { it.reward }, isReroll)

        activeBatch = batch
        activeRoll = roll

        crate.hidePlacedCrate(player, context)

        RollSession.start(roll) { finishedRoll, forced -> onRolled(batch, finishedRoll, forced) }
    }

    private fun onRolled(batch: List<OpenPick>, roll: Roll, forced: Boolean) = finishRemainingPicksOnFailure {
        adopt(roll, batch)
        crate.showPlacedCrate(player, context)

        val pick = batch.singleOrNull()

        when {
            forced || !player.isOnline -> {
                finishAll(false)
            }

            pick != null && crate.canReroll(player, pick) -> {
                ReRollGUI.open(this, pick)
            }

            else -> {
                batch.forEach { give(it) }
                playBatch(batchIndex.get() + 1)
            }
        }
    }

    private fun adopt(roll: Roll, batch: List<OpenPick>) {
        activeRoll = null

        if (batch.size == 1) {
            batch.single().reward = roll.reward
        } else {
            roll.rewards.zip(batch).forEach { (reward, pick) -> pick.reward = reward }
        }
    }

    private fun finish() {
        if (isFinished.compareAndSet(false, true)) {
            OpenSessions.unregister(player)

            if (player.isOnline) {
                runCatching { crate.runAllFinishEffects(player, picks.map { it.reward }) }
                    .onFailure { logOpenFailure(player, it) }
            }
        }
    }
}

private fun logOpenFailure(player: Player, error: Throwable) {
    plugin.logger.log(Level.WARNING, "Error during a crate open for ${player.name}", error)
}

private inline fun OpenSession.finishRemainingPicksOnFailure(block: () -> Unit) {
    runCatching { block() }.onFailure { error ->
        logOpenFailure(player, error)
        finishAll(false)
    }
}
