package com.willfp.ecocrates.crate.open

import com.willfp.eco.core.price.ConfiguredPrice
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.crate.roll.Roll
import com.willfp.ecocrates.crate.roll.RollPickContext
import com.willfp.ecocrates.crate.roll.RollOptions
import com.willfp.ecocrates.event.PickInfo
import com.willfp.ecocrates.pouch.Pouch
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.RewardSource
import org.bukkit.entity.Player

/**
 * What an [OpenSession] needs from the source it opens: a crate or a pouch.
 */
internal sealed interface SessionSource {
    /** The most rewards one roll of this source shows at once. */
    val maxRewards: Int

    /**
     * Makes the roll that shows [rewards], the first of which is the roll's main reward.
     *
     * @param player The opening player.
     * @param context Where and how the open happens.
     * @param rewards The rewards the roll shows.
     * @param isReroll If the roll replays one pick after a reroll.
     * @param pickContext Where [rewards] sit in the open.
     * @return The roll, not yet started.
     */
    fun createRoll(
        player: Player,
        context: OpenContext,
        rewards: List<Reward>,
        isReroll: Boolean,
        pickContext: RollPickContext
    ): Roll

    /**
     * Hides the placed crate of [context] from [player] while a roll plays, if this source asks for it.
     *
     * @param player The opening player.
     * @param context Where and how the open happens.
     */
    fun hidePlacedCrate(player: Player, context: OpenContext)

    /**
     * Shows the placed crate of [context] to [player] again once a roll ends.
     *
     * @param player The opening player.
     * @param context Where and how the open happens.
     */
    fun showPlacedCrate(player: Player, context: OpenContext)

    /**
     * Gives one pick of an open (or queues it if [player] is offline), firing the source's reward
     * event and finish effects.
     *
     * @param player The player receiving the reward.
     * @param pick The pick to give.
     * @param amount The number of rewards in the open.
     */
    fun deliver(player: Player, pick: OpenPick, amount: Int)

    /**
     * Runs the all-finish effects once for every reward of an open.
     *
     * @param player The opening player.
     * @param rewards Every reward of the open.
     */
    fun finishOpen(player: Player, rewards: List<Reward>)

    /**
     * If a reroll can start for [player] now.
     *
     * @param player The opening player.
     * @return If a reroll can start.
     */
    fun canStartReroll(player: Player): Boolean

    /**
     * If [player] can be offered a reroll of [pick] now.
     *
     * @param player The opening player.
     * @param pick The pick to reroll.
     * @return If the reroll can be offered.
     */
    fun canReroll(player: Player, pick: OpenPick): Boolean

    /**
     * The price of the next reroll of [pick].
     *
     * @param pick The pick to reroll.
     * @return The price.
     */
    fun rerollPrice(pick: OpenPick): ConfiguredPrice

    /**
     * Fires the open event for a reroll and counts the reroll as an open.
     *
     * @param player The opening player.
     * @param method How the open was paid for.
     * @param replacement The reward the reroll picked.
     * @param pick Which pick is rerolled.
     * @return The event's reward, or null if the reroll was cancelled.
     */
    fun beginReroll(player: Player, method: OpenMethod, replacement: Reward, pick: PickInfo): Reward?

    companion object {
        /**
         * Find the session adapter for a source.
         *
         * @param source The source being opened.
         * @return The adapter.
         */
        fun forSource(source: RewardSource): SessionSource = when (source) {
            is Crate -> CrateSessionSource(source)
            is Pouch -> PouchSessionSource(source)
            else -> error("${source.sourceType} '${source.id}' cannot open through a session")
        }
    }
}

/**
 * Opens a crate through a session, with the crate's rerolls and placed crate.
 *
 * @param crate The crate being opened.
 */
internal class CrateSessionSource(private val crate: Crate) : SessionSource {
    override val maxRewards: Int
        get() = crate.rollFactory.maxRewards

    override fun createRoll(
        player: Player,
        context: OpenContext,
        rewards: List<Reward>,
        isReroll: Boolean,
        pickContext: RollPickContext
    ): Roll = crate.createRoll(player, context, rewards, isReroll, pickContext)

    override fun hidePlacedCrate(player: Player, context: OpenContext) {
        crate.hidePlacedCrate(player, context)
    }

    override fun showPlacedCrate(player: Player, context: OpenContext) {
        crate.showPlacedCrate(player, context)
    }

    override fun deliver(player: Player, pick: OpenPick, amount: Int) {
        crate.deliver(player = player, pick = pick, amount = amount)
    }

    override fun finishOpen(player: Player, rewards: List<Reward>) {
        crate.runAllFinishEffects(player, rewards)
    }

    override fun canStartReroll(player: Player): Boolean = crate.canStartReroll(player)

    override fun canReroll(player: Player, pick: OpenPick): Boolean = crate.canReroll(player, pick)

    override fun rerollPrice(pick: OpenPick): ConfiguredPrice = crate.rerollPrice(pick)

    override fun beginReroll(player: Player, method: OpenMethod, replacement: Reward, pick: PickInfo): Reward? =
        crate.fireOpenEvent(player, method, replacement, true, pick)?.also { crate.recordOpen(player) }
}

/**
 * Opens a pouch through a session. Pouches have no rerolls and no placed crate.
 *
 * @param pouch The pouch being opened.
 */
internal class PouchSessionSource(private val pouch: Pouch) : SessionSource {
    override val maxRewards: Int
        get() = pouch.rollFactory.maxRewards

    override fun createRoll(
        player: Player,
        context: OpenContext,
        rewards: List<Reward>,
        isReroll: Boolean,
        pickContext: RollPickContext
    ): Roll = pouch.rollFactory.create(
        RollOptions(
            rewards.first(),
            pouch,
            player,
            context.location,
            isReroll,
            context.method,
            context.placedCrate,
            rewards
        ).apply { this.pickContext = pickContext }
    )

    override fun hidePlacedCrate(player: Player, context: OpenContext) = Unit

    override fun showPlacedCrate(player: Player, context: OpenContext) = Unit

    override fun deliver(player: Player, pick: OpenPick, amount: Int) {
        pouch.deliverPick(player = player, pick = pick, amount = amount)
    }

    override fun finishOpen(player: Player, rewards: List<Reward>) {
        pouch.finishOpen(player, rewards)
    }

    override fun canStartReroll(player: Player): Boolean = false

    override fun canReroll(player: Player, pick: OpenPick): Boolean = false

    override fun rerollPrice(pick: OpenPick): ConfiguredPrice = ConfiguredPrice.FREE

    override fun beginReroll(player: Player, method: OpenMethod, replacement: Reward, pick: PickInfo): Reward? = null
}
